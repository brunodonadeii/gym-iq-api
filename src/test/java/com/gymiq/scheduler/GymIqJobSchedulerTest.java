package com.gymiq.scheduler;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.service.PaymentJobService;
import com.gymiq.service.RetentionAlertService;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GymIqJobSchedulerTest {

    @Test
    void refreshOverduePaymentsShouldDelegateToPaymentJobService() {
        PaymentJobService paymentJobService = mock(PaymentJobService.class);
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        GymIqJobScheduler scheduler = new GymIqJobScheduler(paymentJobService, retentionAlertService);

        when(paymentJobService.refreshOverduePayments()).thenReturn(3);

        scheduler.refreshOverduePayments();

        verify(paymentJobService).refreshOverduePayments();
    }

    @Test
    void generateMonthlyPaymentsShouldDelegateToPaymentJobService() {
        PaymentJobService paymentJobService = mock(PaymentJobService.class);
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        GymIqJobScheduler scheduler = new GymIqJobScheduler(paymentJobService, retentionAlertService);

        when(paymentJobService.generateMonthlyPayments()).thenReturn(2);

        scheduler.generateMonthlyPayments();

        verify(paymentJobService).generateMonthlyPayments();
    }

    @Test
    void generateRetentionAlertsShouldDelegateToRetentionService() {
        PaymentJobService paymentJobService = mock(PaymentJobService.class);
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        GymIqJobScheduler scheduler = new GymIqJobScheduler(paymentJobService, retentionAlertService);

        when(retentionAlertService.generateForActiveStudents())
                .thenReturn(List.of(RetentionAlertResponse.fromEntity(TestDataFactory.openRetentionAlert())));

        scheduler.generateRetentionAlerts();

        verify(retentionAlertService).generateForActiveStudents();
    }

    @Test
    void scheduledJobsShouldNotPropagateFailures() {
        PaymentJobService paymentJobService = mock(PaymentJobService.class);
        RetentionAlertService retentionAlertService = mock(RetentionAlertService.class);
        GymIqJobScheduler scheduler = new GymIqJobScheduler(paymentJobService, retentionAlertService);

        when(paymentJobService.refreshOverduePayments()).thenThrow(new RuntimeException("falha"));
        when(paymentJobService.generateMonthlyPayments()).thenThrow(new RuntimeException("falha"));
        when(retentionAlertService.generateForActiveStudents()).thenThrow(new RuntimeException("falha"));

        scheduler.refreshOverduePayments();
        scheduler.generateMonthlyPayments();
        scheduler.generateRetentionAlerts();

        verify(paymentJobService).refreshOverduePayments();
        verify(paymentJobService).generateMonthlyPayments();
        verify(retentionAlertService).generateForActiveStudents();
    }
}
