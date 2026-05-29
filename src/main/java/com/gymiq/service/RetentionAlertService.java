package com.gymiq.service;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.entity.Presence;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import com.gymiq.entity.RetentionAlert.RiskLevel;
import com.gymiq.entity.Student;
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

    private static final int MAX_RISK_SCORE = 100;
    private static final int MAX_INACTIVITY_SCORE = 50;
    private static final int MAX_PAYMENT_SCORE = 50;

    private final RetentionAlertRepository retentionAlertRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PresenceRepository presenceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Optional<RetentionAlertResponse> generateForStudent(Integer studentId) {
        Student student = findActiveStudent(studentId);
        Enrollment activeEnrollment = findActiveEnrollment(studentId);

        Integer inactiveDays = calculateInactiveDays(studentId, activeEnrollment);
        Integer overduePayments = countOverduePayments(studentId);
        Integer riskScore = calculateRiskScore(inactiveDays, overduePayments);

        Optional<RetentionAlert> openAlert = retentionAlertRepository
                .findByStudentStudentIdAndStatus(studentId, AlertStatus.OPEN);

        if (!hasActionableRisk(riskScore)) {
            openAlert.ifPresent(this::resolveAutomatically);
            log.info("Retention alert not generated: student={}, score={}", studentId, riskScore);
            return Optional.empty();
        }

        RiskLevel riskLevel = resolveRiskLevel(riskScore);
        String message = buildMessage(inactiveDays, overduePayments, riskLevel);

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
    public List<RetentionAlertResponse> generateForOverdueStudents() {
        List<Integer> studentIds = paymentRepository.findActiveStudentIdsWithOverduePayments(
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
    public Page<RetentionAlertResponse> findByStudent(Integer studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return retentionAlertRepository.findByStudentStudentId(studentId, pageable)
                .map(RetentionAlertResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public RetentionAlertResponse findById(Integer id) {
        return RetentionAlertResponse.fromEntity(findEntityById(id));
    }

    @Transactional
    public RetentionAlertResponse resolve(Integer id) {
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

    private RetentionAlert findEntityById(Integer id) {
        return retentionAlertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta de retencao nao encontrado: " + id));
    }

    private Student findActiveStudent(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado: " + studentId));

        if (Boolean.FALSE.equals(student.getUser().getActive())) {
            throw new BusinessException("Aluno inativo nao deve gerar alerta de retencao");
        }
        return student;
    }

    private Enrollment findActiveEnrollment(Integer studentId) {
        return enrollmentRepository.findByStudentStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Aluno sem matricula ativa nao deve gerar alerta de retencao"));
    }

    private Integer calculateInactiveDays(Integer studentId, Enrollment activeEnrollment) {
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

    private Integer countOverduePayments(Integer studentId) {
        long manuallyMarkedOverdue = paymentRepository
                .countByEnrollmentStudentStudentIdAndStatus(studentId, PaymentStatus.OVERDUE);
        long pendingPastDue = paymentRepository
                .countByEnrollmentStudentStudentIdAndStatusAndDueDateBefore(
                        studentId,
                        PaymentStatus.PENDING,
                        LocalDate.now());

        return Math.toIntExact(manuallyMarkedOverdue + pendingPastDue);
    }

    private Integer calculateRiskScore(Integer inactiveDays, Integer overduePayments) {
        int inactivityScore = calculateInactivityScore(inactiveDays);
        int paymentScore = Math.min(overduePayments * 20, MAX_PAYMENT_SCORE);
        return Math.min(inactivityScore + paymentScore, MAX_RISK_SCORE);
    }

    private boolean hasActionableRisk(Integer riskScore) {
        return riskScore > 0;
    }

    private Integer calculateInactivityScore(Integer inactiveDays) {
        if (inactiveDays >= 30) {
            return MAX_INACTIVITY_SCORE;
        }
        if (inactiveDays >= 15) {
            return 35;
        }
        if (inactiveDays >= 8) {
            return 20;
        }
        return 0;
    }

    private RiskLevel resolveRiskLevel(Integer riskScore) {
        if (riskScore >= 80) {
            return RiskLevel.CRITICAL;
        }
        if (riskScore >= 60) {
            return RiskLevel.HIGH;
        }
        if (riskScore >= 30) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String buildMessage(Integer inactiveDays, Integer overduePayments, RiskLevel riskLevel) {
        String inactivityText = inactiveDays + " dia(s) sem check-in";

        return "Risco " + riskLevel.name() + ": " +
                inactivityText + " e " +
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

    private void generateAlertSafely(Integer studentId, List<RetentionAlertResponse> generatedAlerts) {
        try {
            generateForStudent(studentId).ifPresent(generatedAlerts::add);
        } catch (BusinessException | ResourceNotFoundException ex) {
            log.warn("Retention alert skipped for student={} reason={}", studentId, ex.getMessage());
        }
    }
}
