package com.gymiq.service;

import com.gymiq.dto.request.CreatePaymentRequest;
import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matricula nao encontrada: " + request.getEnrollmentId()));

        if (enrollment.getStatus() == EnrollmentStatus.CANCELED) {
            throw new BusinessException("Nao e possivel criar pagamento para matricula cancelada");
        }

        BigDecimal amount = request.getAmount() != null
                ? request.getAmount()
                : enrollment.getPlan().getMonthlyPrice();

        Payment payment = Payment.builder()
                .enrollment(enrollment)
                .amount(amount)
                .dueDate(request.getDueDate())
                .status(resolveInitialStatus(request.getDueDate()))
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        paymentRepository.save(payment);
        log.info("Pagamento criado: id={}, matricula={}, valor={}, vencimento={}",
                payment.getPaymentId(), enrollment.getEnrollmentId(), amount, payment.getDueDate());

        return PaymentResponse.fromEntity(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Integer id) {
        return PaymentResponse.fromEntity(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByEnrollment(Integer enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException("Matricula nao encontrada: " + enrollmentId);
        }

        return paymentRepository.findByEnrollmentEnrollmentId(enrollmentId)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByStudent(Integer studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Aluno nao encontrado: " + studentId);
        }

        return paymentRepository.findByEnrollmentStudentStudentId(studentId)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findOverdue() {
        return paymentRepository.findByStatus(PaymentStatus.OVERDUE)
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
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

    @Transactional
    public List<PaymentResponse> refreshOverdue() {
        List<Payment> overduePayments = paymentRepository
                .findByStatusAndDueDateBefore(PaymentStatus.PENDING, LocalDate.now());

        overduePayments.forEach(payment -> payment.setStatus(PaymentStatus.OVERDUE));
        paymentRepository.saveAll(overduePayments);

        return overduePayments.stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    private Payment findEntityById(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado: " + id));
    }

    private PaymentStatus resolveInitialStatus(LocalDate dueDate) {
        return dueDate.isBefore(LocalDate.now()) ? PaymentStatus.OVERDUE : PaymentStatus.PENDING;
    }
}
