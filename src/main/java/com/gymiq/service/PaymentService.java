package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final PersonalDataProtectionService personalDataProtectionService;

    @Transactional
    public Payment createFirstPaymentForEnrollment(Enrollment enrollment) {
        LocalDate dueDate = enrollment.getStartDate();

        if (paymentRepository.existsByEnrollmentEnrollmentIdAndDueDate(
                enrollment.getEnrollmentId(), dueDate)) {
            log.info("Pagamento inicial já existe para matrícula id={} e vencimento={}",
                    enrollment.getEnrollmentId(), dueDate);
            return null;
        }

        if (paymentRepository.existsByStudentPlanAndDueDate(
                enrollment.getStudent().getStudentId(),
                enrollment.getPlan().getPlanId(),
                dueDate)) {
            log.info("Pagamento inicial já existe para aluno={}, plano={} e vencimento={}",
                    enrollment.getStudent().getStudentId(), enrollment.getPlan().getPlanId(), dueDate);
            return null;
        }

        Payment payment = Payment.builder()
                .enrollment(enrollment)
                .amount(enrollment.getPlan().getMonthlyPrice())
                .dueDate(enrollment.getStartDate())
                .status(resolveInitialStatus(enrollment.getStartDate()))
                .notes("Primeira mensalidade da matrícula")
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = status == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(status, pageable);

        return payments.map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return PaymentResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByEnrollment(UUID enrollmentId, Pageable pageable) {
        return findByEnrollment(enrollmentId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByEnrollment(UUID enrollmentId, PaymentStatus status, Pageable pageable) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException("Matrícula não encontrada: " + enrollmentId);
        }

        Page<Payment> payments = status == null
                ? paymentRepository.findByEnrollmentEnrollmentId(enrollmentId, pageable)
                : paymentRepository.findByEnrollmentEnrollmentIdAndStatus(enrollmentId, status, pageable);

        return payments.map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByStudent(UUID studentId, Pageable pageable) {
        return findByStudent(studentId, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByStudent(UUID studentId, PaymentStatus status, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno não encontrado: " + studentId);
        }

        Page<Payment> payments = status == null
                ? paymentRepository.findByEnrollmentStudentStudentId(studentId, pageable)
                : paymentRepository.findByEnrollmentStudentStudentIdAndStatus(studentId, status, pageable);

        return payments.map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByAuthenticatedStudent(String email, Pageable pageable) {
        return findByAuthenticatedStudent(email, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByAuthenticatedStudent(String email, PaymentStatus status, Pageable pageable) {
        UUID studentId = studentRepository.findByUserEmailHash(personalDataProtectionService.emailHash(email))
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado para o usuário autenticado"))
                .getStudentId();

        return findByStudent(studentId, status, pageable);
    }

    @Transactional
    @Auditable(action = AuditAction.PAY_PAYMENT, resourceType = ResourceType.PAYMENT, description = "Quitou pagamento")
    public PaymentResponse pay(UUID id, PayPaymentRequest request) {
        Payment payment = findEntityById(id);
        PayPaymentRequest payRequest = request != null ? request : new PayPaymentRequest();

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Pagamento já foi marcado como pago");
        }

        validatePaymentMethod(payRequest);

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(payRequest.getPaidAt() != null ? payRequest.getPaidAt() : now());
        payment.setPaymentMethod(payRequest.getPaymentMethod().trim());
        if (payRequest.getNotes() != null) {
            payment.setNotes(payRequest.getNotes());
        }

        paymentRepository.save(payment);
        log.info("Pagamento quitado: id={}, pagoEm={}", payment.getPaymentId(), payment.getPaidAt());

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    @Auditable(action = AuditAction.CHANGE_PAYMENT_STATUS, resourceType = ResourceType.PAYMENT, description = "Alterou status do pagamento")
    public PaymentResponse changeStatus(UUID id, PaymentStatus newStatus) {
        Payment payment = findEntityById(id);

        if (newStatus == PaymentStatus.PAID) {
            throw new BusinessException("Use a rota de quitação para marcar pagamento como pago");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Não é possível alterar status de pagamento já quitado");
        }
        if (newStatus == PaymentStatus.OVERDUE && payment.getDueDate().isAfter(today())) {
            throw new BusinessException("Não é possível marcar como atrasado um pagamento com vencimento futuro");
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        log.info("Status do pagamento id={} alterado para {}", id, newStatus);

        return PaymentResponse.fromEntity(payment);
    }

    private void validatePaymentMethod(PayPaymentRequest request) {
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new BusinessException("Método de pagamento é obrigatório para quitar o pagamento");
        }
    }

    private Payment findEntityById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + id));
    }

    private PaymentStatus resolveInitialStatus(LocalDate dueDate) {
        return dueDate.isBefore(today()) ? PaymentStatus.OVERDUE : PaymentStatus.PENDING;
    }

    private LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
