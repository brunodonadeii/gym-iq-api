package com.gymiq.service;

import java.util.UUID;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import com.gymiq.entity.RetentionAlert.RiskLevel;
import com.gymiq.entity.Student;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.RetentionAlertRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionAlertService {

    private static final int INACTIVITY_THRESHOLD_LOW = 8;
    private static final int INACTIVITY_THRESHOLD_MEDIUM = 15;
    private static final int INACTIVITY_THRESHOLD_HIGH = 30;

    private static final int INACTIVITY_SCORE_LOW = 10;
    private static final int INACTIVITY_SCORE_MEDIUM = 25;
    private static final int INACTIVITY_SCORE_HIGH = 40;

    private static final int FREQUENCY_WINDOW_DAYS = 30;
    private static final int FREQUENCY_MINIMUM_HEALTHY = 8;
    private static final int FREQUENCY_WARNING = 4;

    private static final int FREQUENCY_SCORE_MEDIUM = 15;
    private static final int FREQUENCY_SCORE_HIGH = 30;

    private static final int POINTS_PER_OVERDUE_PAYMENT = 15;

    private static final int MAX_RISK_SCORE = 100;
    private static final int MAX_PAYMENT_SCORE = 30;

    private static final int RISK_THRESHOLD_MEDIUM = 30;
    private static final int RISK_THRESHOLD_HIGH = 60;
    private static final int RISK_THRESHOLD_CRITICAL = 80;

    private final RetentionAlertRepository retentionAlertRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PresenceRepository presenceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    @Auditable(action = AuditAction.GENERATE_RETENTION_ALERT, resourceType = ResourceType.STUDENT, description = "Processou geracao de alerta de retencao para aluno")
    public Optional<RetentionAlertResponse> generateForStudent(UUID studentId) {
        Student student = findActiveStudent(studentId);
        Enrollment activeEnrollment = findActiveEnrollment(studentId);

        Integer inactiveDays = calculateInactiveDays(studentId, activeEnrollment);
        Integer recentCheckIns = countRecentCheckIns(studentId, activeEnrollment);
        Integer frequencyWindowDays = calculateFrequencyWindowDays(activeEnrollment);
        Integer overduePayments = countOverduePayments(studentId);
        Integer riskScore = calculateRiskScore(inactiveDays, recentCheckIns, frequencyWindowDays, overduePayments);

        Optional<RetentionAlert> openAlert = retentionAlertRepository
                .findByStudentStudentIdAndStatus(studentId, AlertStatus.OPEN);

        if (!hasActionableRisk(riskScore)) {
            openAlert.ifPresent(this::resolveAutomatically);
            log.info("Retention alert not generated: student={}, score={}", studentId, riskScore);
            return Optional.empty();
        }

        RiskLevel riskLevel = resolveRiskLevel(riskScore);
        String message = buildMessage(inactiveDays, recentCheckIns, frequencyWindowDays, overduePayments, riskLevel);

        RetentionAlert alert = openAlert
                .orElseGet(() -> RetentionAlert.builder()
                        .student(student)
                        .status(AlertStatus.OPEN)
                        .build());

        updateAlert(alert, riskScore, riskLevel, inactiveDays, overduePayments, message);
        retentionAlertRepository.save(alert);

        log.info("Retention alert generated: student={}, score={}, level={}",
                studentId, riskScore, riskLevel);
        return Optional.of(RetentionAlertResponse.fromEntity(alert));
    }

    @Transactional
    @Auditable(action = AuditAction.GENERATE_RETENTION_ALERTS, resourceType = ResourceType.JOB, description = "Processou alertas para alunos ativos")
    public List<RetentionAlertResponse> generateForActiveStudents() {
        List<RetentionAlertResponse> generatedAlerts = new ArrayList<>();

        enrollmentRepository.findByStatus(EnrollmentStatus.ACTIVE)
                .stream()
                .map(Enrollment::getStudent)
                .filter(student -> Boolean.TRUE.equals(student.getUser().getActive()))
                .map(Student::getStudentId)
                .distinct()
                .forEach(studentId -> generateAlertSafely(studentId, generatedAlerts));

        return generatedAlerts;
    }

    @Transactional
    @Auditable(action = AuditAction.GENERATE_RETENTION_ALERTS, resourceType = ResourceType.JOB, description = "Processou alertas para alunos inadimplentes")
    public List<RetentionAlertResponse> generateForOverdueStudents() {
        List<UUID> studentIds = paymentRepository.findActiveStudentIdsWithOverduePayments(
                EnrollmentStatus.ACTIVE,
                PaymentStatus.OVERDUE,
                PaymentStatus.PENDING,
                LocalDate.now());

        log.info("Generating retention alerts for {} student(s) with overdue payments", studentIds.size());

        List<RetentionAlertResponse> generatedAlerts = new ArrayList<>();
        studentIds.forEach(studentId -> generateAlertSafely(studentId, generatedAlerts));
        return generatedAlerts;
    }

    @Transactional(readOnly = true)
    public Page<RetentionAlertResponse> findOpenAlerts(Pageable pageable) {
        return retentionAlertRepository.findByStatus(AlertStatus.OPEN, pageable)
                .map(RetentionAlertResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<RetentionAlertResponse> findByStudent(UUID studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return retentionAlertRepository.findByStudentStudentId(studentId, pageable)
                .map(RetentionAlertResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public RetentionAlertResponse findById(UUID id) {
        return RetentionAlertResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    @Auditable(action = AuditAction.RESOLVE_RETENTION_ALERT, resourceType = ResourceType.RETENTION_ALERT, description = "Resolveu alerta de retencao")
    public RetentionAlertResponse resolve(UUID id) {
        RetentionAlert alert = findEntityById(id);

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new BusinessException("Alerta ja foi resolvido");
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        retentionAlertRepository.save(alert);

        log.info("Retention alert resolved: id={}", id);
        return RetentionAlertResponse.fromEntity(alert);
    }

    private RetentionAlert findEntityById(UUID id) {
        return retentionAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta de retencao nao encontrado: " + id));
    }

    private Student findActiveStudent(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + studentId));

        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Aluno inativo nao deve gerar alerta de retencao");
        }
        return student;
    }

    private Enrollment findActiveEnrollment(UUID studentId) {
        return enrollmentRepository.findByStudentStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Aluno sem matricula ativa nao deve gerar alerta de retencao"));
    }

    private Integer calculateInactiveDays(UUID studentId, Enrollment activeEnrollment) {
        LocalDate today = LocalDate.now();
        LocalDate enrollmentStartDate = activeEnrollment.getStartDate();

        return presenceRepository
                .findFirstByStudentStudentIdAndCheckInAtGreaterThanEqualOrderByCheckInAtDesc(
                        studentId,
                        enrollmentStartDate.atStartOfDay())
                .map(Presence::getCheckInAt)
                .map(checkInAt -> calculateDaysBetween(checkInAt.toLocalDate(), today))
                .orElseGet(() -> calculateDaysBetween(enrollmentStartDate, today));
    }

    private Integer calculateDaysBetween(LocalDate startDate, LocalDate endDate) {
        return Math.toIntExact(Math.max(0, ChronoUnit.DAYS.between(startDate, endDate)));
    }

    private Integer countOverduePayments(UUID studentId) {
        long manuallyMarkedOverdue = paymentRepository
                .countByEnrollmentStudentStudentIdAndStatus(studentId, PaymentStatus.OVERDUE);
        long pendingPastDue = paymentRepository
                .countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                        studentId,
                        PaymentStatus.PENDING,
                        LocalDate.now());

        return Math.toIntExact(manuallyMarkedOverdue + pendingPastDue);
    }

    private Integer countRecentCheckIns(UUID studentId, Enrollment activeEnrollment) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = activeEnrollment.getStartDate()
                .atStartOfDay()
                .isAfter(now.minusDays(FREQUENCY_WINDOW_DAYS))
                ? activeEnrollment.getStartDate().atStartOfDay()
                : now.minusDays(FREQUENCY_WINDOW_DAYS);

        return Math.toIntExact(presenceRepository
                .countByStudentStudentIdAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                        studentId,
                        startDate,
                        now));
    }

    private Integer calculateFrequencyWindowDays(Enrollment activeEnrollment) {
        LocalDate windowStartDate = LocalDate.now().minusDays(FREQUENCY_WINDOW_DAYS);
        LocalDate effectiveStartDate = activeEnrollment.getStartDate().isAfter(windowStartDate)
                ? activeEnrollment.getStartDate()
                : windowStartDate;

        return calculateDaysBetween(effectiveStartDate, LocalDate.now());
    }

    private Integer calculateRiskScore(
            Integer inactiveDays,
            Integer recentCheckIns,
            Integer frequencyWindowDays,
            Integer overduePayments) {
        int inactivityScore = calculateInactivityScore(inactiveDays);
        int frequencyScore = calculateFrequencyScore(recentCheckIns, frequencyWindowDays);
        int paymentScore = Math.min(overduePayments * POINTS_PER_OVERDUE_PAYMENT, MAX_PAYMENT_SCORE);
        return Math.min(inactivityScore + frequencyScore + paymentScore, MAX_RISK_SCORE);
    }

    private boolean hasActionableRisk(Integer riskScore) {
        return riskScore > 0;
    }

    private Integer calculateInactivityScore(Integer inactiveDays) {
        if (inactiveDays >= INACTIVITY_THRESHOLD_HIGH) {
            return INACTIVITY_SCORE_HIGH;
        }
        if (inactiveDays >= INACTIVITY_THRESHOLD_MEDIUM) {
            return INACTIVITY_SCORE_MEDIUM;
        }
        if (inactiveDays >= INACTIVITY_THRESHOLD_LOW) {
            return INACTIVITY_SCORE_LOW;
        }
        return 0;
    }

    private Integer calculateFrequencyScore(Integer recentCheckIns, Integer frequencyWindowDays) {
        if (frequencyWindowDays < INACTIVITY_THRESHOLD_LOW) {
            return 0;
        }

        int healthyThreshold = calculateProportionalFrequencyThreshold(
                FREQUENCY_MINIMUM_HEALTHY,
                frequencyWindowDays);
        int warningThreshold = calculateProportionalFrequencyThreshold(
                FREQUENCY_WARNING,
                frequencyWindowDays);

        if (recentCheckIns < warningThreshold) {
            return FREQUENCY_SCORE_HIGH;
        }
        if (recentCheckIns < healthyThreshold) {
            return FREQUENCY_SCORE_MEDIUM;
        }
        return 0;
    }

    private Integer calculateProportionalFrequencyThreshold(Integer baseThreshold, Integer frequencyWindowDays) {
        return Math.max(1, (int) Math.ceil(baseThreshold * (frequencyWindowDays / (double) FREQUENCY_WINDOW_DAYS)));
    }

    private RiskLevel resolveRiskLevel(Integer riskScore) {
        if (riskScore >= RISK_THRESHOLD_CRITICAL) {
            return RiskLevel.CRITICAL;
        }
        if (riskScore >= RISK_THRESHOLD_HIGH) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= RISK_THRESHOLD_MEDIUM) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String buildMessage(
            Integer inactiveDays,
            Integer recentCheckIns,
            Integer frequencyWindowDays,
            Integer overduePayments,
            RiskLevel riskLevel) {
        String inactivityText = inactiveDays + " dia(s) sem check-in";

        return "Risco " + riskLevel.name() + ": " +
                inactivityText + ", " +
                recentCheckIns + " check-in(s) nos ultimos " +
                frequencyWindowDays + " dia(s) avaliados e " +
                overduePayments + " pagamento(s) atrasado(s).";
    }

    private void updateAlert(
            RetentionAlert alert,
            Integer riskScore,
            RiskLevel riskLevel,
            Integer inactiveDays,
            Integer overduePayments,
            String message) {

        alert.setRiskScore(riskScore);
        alert.setRiskLevel(riskLevel);
        alert.setInactiveDays(inactiveDays);
        alert.setOverduePayments(overduePayments);
        alert.setMessage(message);
        alert.setResolvedAt(null);
    }

    private void resolveAutomatically(RetentionAlert alert) {
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        retentionAlertRepository.save(alert);
        log.info("Retention alert automatically resolved: id={}", alert.getRetentionAlertId());
    }

    private void generateAlertSafely(UUID studentId, List<RetentionAlertResponse> generatedAlerts) {
        try {
            generateForStudent(studentId).ifPresent(generatedAlerts::add);
        } catch (BusinessException | ResourceNotFoundException ex) {
            log.warn("Retention alert skipped for student={} reason={}", studentId, ex.getMessage());
        }
    }
}
