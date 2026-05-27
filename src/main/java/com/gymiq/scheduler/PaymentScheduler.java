package com.gymiq.scheduler;

import com.gymiq.service.PaymentJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentJobService paymentJobService;

    @Scheduled(cron = "0 15 0 * * *", zone = "America/Sao_Paulo")
    public void refreshOverduePayments() {
        log.info("Scheduler iniciado: refreshOverduePayments");
        paymentJobService.refreshOverduePayments();
    }

    @Scheduled(cron = "0 20 0 * * *", zone = "America/Sao_Paulo")
    public void generateMonthlyPayments() {
        log.info("Scheduler iniciado: generateMonthlyPayments");
        paymentJobService.generateMonthlyPayments();
    }
}
