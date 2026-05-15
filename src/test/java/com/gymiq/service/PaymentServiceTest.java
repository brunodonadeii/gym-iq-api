package com.gymiq.service;

import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createFirstPaymentForEnrollmentShouldPersistPendingPayment() {
        Enrollment enrollment = TestDataFactory.activeEnrollment();

        when(paymentRepository.existsByEnrollmentEnrollmentIdAndDueDate(
                enrollment.getEnrollmentId(), enrollment.getStartDate())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createFirstPaymentForEnrollment(enrollment);

        assertThat(payment.getEnrollment()).isEqualTo(enrollment);
        assertThat(payment.getAmount()).isEqualByComparingTo(enrollment.getPlan().getMonthlyPrice());
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.OVERDUE);
    }

    @Test
    void payShouldMarkPaymentAsPaid() {
        Payment payment = TestDataFactory.pendingPayment();
        LocalDateTime paidAt = LocalDateTime.of(2026, 5, 15, 10, 30);
        PayPaymentRequest request = new PayPaymentRequest();
        request.setPaidAt(paidAt);
        request.setPaymentMethod("PIX");

        when(paymentRepository.findById(payment.getPaymentId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.pay(payment.getPaymentId(), request);

        assertThat(response.getStatus()).isEqualTo("PAID");
        assertThat(response.getPaidAt()).isEqualTo(paidAt);
        assertThat(response.getPaymentMethod()).isEqualTo("PIX");
        verify(paymentRepository).save(payment);
    }

    @Test
    void payShouldRejectAlreadyPaidPayment() {
        Payment payment = TestDataFactory.pendingPayment();
        payment.setStatus(Payment.PaymentStatus.PAID);

        when(paymentRepository.findById(payment.getPaymentId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.pay(payment.getPaymentId(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pago");
    }

    @Test
    void refreshOverdueShouldMovePastDuePendingPaymentsToOverdue() {
        Payment payment = TestDataFactory.pendingPayment();
        payment.setDueDate(LocalDate.now().minusDays(1));

        when(paymentRepository.findByStatusAndDueDateBefore(Payment.PaymentStatus.PENDING, LocalDate.now()))
                .thenReturn(List.of(payment));

        List<PaymentResponse> responses = paymentService.refreshOverdue();

        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.OVERDUE);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("OVERDUE");
        verify(paymentRepository).saveAll(List.of(payment));
    }
}
