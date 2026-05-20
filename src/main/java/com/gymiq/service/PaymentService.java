package com.gymiq.service;

import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Payment.PaymentStatus;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public Payment createFirstPaymentForEnrollment(Enrollment enrollment) {
        LocalDate dueDate = enrollment.getStartDate();

        if (paymentRepository.existsByEnrollmentEnrollmentIdAndDueDate(
                enrollment.getEnrollmentId(), dueDate)) {
            log.info("Pagamento inicial ja existe para matricula id={} e vencimento={}",
                    enrollment.getEnrollmentId(), dueDate);
            return null;
        }

        Payment payment = Payment.builder()
                .enrollment(enrollment)
                .amount(enrollment.getPlan().getMonthlyPrice())
                .dueDate(enrollment.getStartDate())
                .status(resolveInitialStatus(enrollment.getStartDate()))
                .notes("Primeira mensalidade da matricula")
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Integer id) {
        return PaymentResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByEnrollment(Integer enrollmentId, Pageable pageable) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException("Matricula nao encontrada: " + enrollmentId);
        }

        return paymentRepository.findByEnrollmentEnrollmentId(enrollmentId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByStudent(Integer studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return paymentRepository.findByEnrollmentStudentStudentId(studentId, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findOverdue(Pageable pageable) {
        return paymentRepository.findByStatus(PaymentStatus.OVERDUE, pageable)
                .map(PaymentResponse::fromEntity);
    }

    @Transactional
    public PaymentResponse pay(Integer id, PayPaymentRequest request) {
        Payment payment = findEntityById(id);
        PayPaymentRequest payRequest = request != null ? request : new PayPaymentRequest();

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Pagamento ja foi marcado como pago");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(payRequest.getPaidAt() != null ? payRequest.getPaidAt() : LocalDateTime.now());

        if (payRequest.getPaymentMethod() != null && !payRequest.getPaymentMethod().isBlank()) {
            payment.setPaymentMethod(payRequest.getPaymentMethod());
        }
        if (payRequest.getNotes() != null) {
            payment.setNotes(payRequest.getNotes());
        }

        paymentRepository.save(payment);
        log.info("Pagamento quitado: id={}, pagoEm={}", payment.getPaymentId(), payment.getPaidAt());

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional
    public PaymentResponse changeStatus(Integer id, PaymentStatus newStatus) {
        Payment payment = findEntityById(id);

        if (newStatus == PaymentStatus.PAID) {
            throw new BusinessException("Use a rota de quitacao para marcar pagamento como pago");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Nao e possivel alterar status de pagamento ja quitado");
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        log.info("Status do pagamento id={} alterado para {}", id, newStatus);

        return PaymentResponse.fromEntity(payment);
    }

    private Payment findEntityById(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado: " + id));
    }

    private PaymentStatus resolveInitialStatus(LocalDate dueDate) {
        return dueDate.isBefore(LocalDate.now()) ? PaymentStatus.OVERDUE : PaymentStatus.PENDING;
    }
}
