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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionAlertService {

    private static final int MAX_RISK_SCORE = 100;
    private static final int MAX_INACTIVITY_SCORE = 50;
    private static final int MAX_PAYMENT_SCORE = 50;
    private static final int NO_CHECK_IN_DAYS = 999;

    private final RetentionAlertRepository retentionAlertRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PresenceRepository presenceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public RetentionAlertResponse generateForStudent(Integer studentId) {
        Student student = findActiveStudent(studentId);
        ensureStudentHasActiveEnrollment(studentId);

        Integer inactiveDays = calculateInactiveDays(studentId);
        Integer overduePayments = countOverduePayments(studentId);
        Integer riskScore = calculateRiskScore(inactiveDays, overduePayments);
        RiskLevel riskLevel = resolveRiskLevel(riskScore);
        String message = buildMessage(inactiveDays, overduePayments, riskLevel);

        RetentionAlert alert = retentionAlertRepository
                .findByStudentStudentIdAndStatus(studentId, AlertStatus.OPEN)
                .orElseGet(() -> RetentionAlert.builder()
                        .student(student)
                        .status(AlertStatus.OPEN)
                        .build());

        updateAlert(alert, riskScore, riskLevel, inactiveDays, overduePayments, message);
        retentionAlertRepository.save(alert);

        log.info("Retention alert generated: student={}, score={}, level={}",
                studentId, riskScore, riskLevel);
        return RetentionAlertResponse.fromEntity(alert);
    }

    @Transactional
    public List<RetentionAlertResponse> generateForActiveStudents() {
        return enrollmentRepository.findByStatus(EnrollmentStatus.ACTIVE)
                .stream()
                .map(Enrollment::getStudent)
                .map(Student::getStudentId)
                .distinct()
                .map(this::generateForStudent)
                .toList();
    }

    @Transactional
    public List<RetentionAlertResponse> generateForOverdueStudents() {
        List<Integer> studentIds = paymentRepository.findActiveStudentIdsWithOverduePayments(
                EnrollmentStatus.ACTIVE,
                PaymentStatus.OVERDUE,
                PaymentStatus.PENDING,
                LocalDate.now());

        log.info("Generating retention alerts for {} student(s) with overdue payments", studentIds.size());

        return studentIds.stream()
                .map(this::generateForStudent)
                .toList();
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

    private void ensureStudentHasActiveEnrollment(Integer studentId) {
        if (!enrollmentRepository.existsByStudentStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Aluno sem matricula ativa nao deve gerar alerta de retencao");
        }
    }

    private Integer calculateInactiveDays(Integer studentId) {
        return presenceRepository.findFirstByStudentStudentIdOrderByCheckInAtDesc(studentId)
                .map(Presence::getCheckInAt)
                .map(checkInAt -> ChronoUnit.DAYS.between(checkInAt.toLocalDate(), LocalDate.now()))
                .map(Long::intValue)
                .orElse(NO_CHECK_IN_DAYS);
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
        String inactivityText = inactiveDays.equals(NO_CHECK_IN_DAYS)
                ? "sem historico de check-in"
                : inactiveDays + " dia(s) sem check-in";

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
}
