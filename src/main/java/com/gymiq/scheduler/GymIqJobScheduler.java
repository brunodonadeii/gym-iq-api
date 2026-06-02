package com.gymiq.scheduler;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.service.PaymentJobService;
import com.gymiq.service.RetentionAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GymIqJobScheduler {

    private final PaymentJobService paymentJobService;
    private final RetentionAlertService retentionAlertService;

    @Scheduled(cron = "${gymiq.scheduler.payments.refresh-overdue-cron}", zone = "America/Sao_Paulo")
    public void refreshOverduePayments() {
        try {
            int affectedPayments = paymentJobService.refreshOverduePayments();
            log.info("Scheduled job completed: refresh-overdue affectedPayments={}", affectedPayments);
        } catch (Exception exception) {
            log.error("Scheduled job failed: refresh-overdue", exception);
        }
    }

    @Scheduled(cron = "${gymiq.scheduler.payments.generate-monthly-cron}", zone = "America/Sao_Paulo")
    public void generateMonthlyPayments() {
        try {
            int createdPayments = paymentJobService.generateMonthlyPayments();
            log.info("Scheduled job completed: generate-monthly createdPayments={}", createdPayments);
        } catch (Exception exception) {
            log.error("Scheduled job failed: generate-monthly", exception);
        }
    }

    @Scheduled(cron = "${gymiq.scheduler.retention.generate-active-students-cron}", zone = "America/Sao_Paulo")
    public void generateRetentionAlerts() {
        try {
            List<RetentionAlertResponse> generatedAlerts = retentionAlertService.generateForActiveStudents();
            log.info("Scheduled job completed: generate-retention-alerts generatedAlerts={}", generatedAlerts.size());
        } catch (Exception exception) {
            log.error("Scheduled job failed: generate-retention-alerts", exception);
        }
    }
}
