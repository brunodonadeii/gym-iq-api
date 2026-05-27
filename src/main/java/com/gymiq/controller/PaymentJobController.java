package com.gymiq.controller;

import com.gymiq.service.PaymentJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @RequestMapping(
            value = "/refresh-overdue",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> refreshOverdue(
            @RequestHeader(value = "X-Job-Secret", required = false) String headerSecret,
            @RequestParam(value = "secret", required = false) String querySecret) {
        if (!isAuthorized(headerSecret, querySecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        }

        int affectedPayments = paymentJobService.refreshOverduePayments();
        return ResponseEntity.ok(buildResponse("refresh-overdue", affectedPayments));
    }

    @RequestMapping(
            value = "/generate-monthly",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateMonthly(
            @RequestHeader(value = "X-Job-Secret", required = false) String headerSecret,
            @RequestParam(value = "secret", required = false) String querySecret) {
        if (!isAuthorized(headerSecret, querySecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        }

        int affectedPayments = paymentJobService.generateMonthlyPayments();
        return ResponseEntity.ok(buildResponse("generate-monthly", affectedPayments));
    }

    private boolean isAuthorized(String headerSecret, String querySecret) {
        String secret = headerSecret != null && !headerSecret.isBlank() ? headerSecret : querySecret;
        return jobSecret != null
                && !jobSecret.isBlank()
                && jobSecret.equals(secret);
    }

    private String buildResponse(String job, int affectedPayments) {
        return "ok job=" + job +
                " affectedPayments=" + affectedPayments +
                " executedAt=" + LocalDateTime.now();
    }
}
