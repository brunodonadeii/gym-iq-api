package com.gymiq.service;

import com.gymiq.entity.Payment;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentJobServiceTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private PaymentJobService paymentJobService;

    @Test
    void refreshOverduePaymentsShouldMovePastDuePendingPaymentsToOverdue() {
        Payment payment = TestDataFactory.pendingPayment();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        payment.setDueDate(today.minusDays(1));

        when(paymentRepository.findByStatusAndDueDateBefore(Payment.PaymentStatus.PENDING, today))
                .thenReturn(List.of(payment));

        int affectedPayments = paymentJobService.refreshOverduePayments();

        assertThat(affectedPayments).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.OVERDUE);
        verify(paymentRepository).saveAll(List.of(payment));
    }
}
