package com.gymiq.controller;

import com.gymiq.dto.response.PaymentJobResponse;
import com.gymiq.service.PaymentJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/jobs/payments")
@RequiredArgsConstructor
public class PaymentJobController {

    private final PaymentJobService paymentJobService;

    @Value("${gymiq.jobs.secret}")
    private String jobSecret;

    @PostMapping("/refresh-overdue")
    public ResponseEntity<PaymentJobResponse> refreshOverdue(
            @RequestHeader(value = "X-Job-Secret", required = false) String secret) {
        if (!isAuthorized(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int affectedPayments = paymentJobService.refreshOverduePayments();
        return ResponseEntity.ok(buildResponse("refresh-overdue", affectedPayments));
    }

    @PostMapping("/generate-monthly")
    public ResponseEntity<PaymentJobResponse> generateMonthly(
            @RequestHeader(value = "X-Job-Secret", required = false) String secret) {
        if (!isAuthorized(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int affectedPayments = paymentJobService.generateMonthlyPayments();
        return ResponseEntity.ok(buildResponse("generate-monthly", affectedPayments));
    }

    private boolean isAuthorized(String secret) {
        return jobSecret != null
                && !jobSecret.isBlank()
                && jobSecret.equals(secret);
    }

    private PaymentJobResponse buildResponse(String job, int affectedPayments) {
        return PaymentJobResponse.builder()
                .job(job)
                .affectedPayments(affectedPayments)
                .executedAt(LocalDateTime.now())
                .build();
    }
}
