package com.gymiq.controller;

import com.gymiq.service.RetentionAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/jobs/retention")
@RequiredArgsConstructor
public class RetentionJobController {

    private final RetentionAlertService retentionAlertService;

    @Value("${gymiq.jobs.secret}")
    private String jobSecret;

    @RequestMapping(
            value = "/generate-active-students",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateActiveStudents(
            @RequestHeader(value = "X-Job-Secret", required = false) String headerSecret,
            @RequestParam(value = "secret", required = false) String querySecret) {
        if (!isAuthorized(headerSecret, querySecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        }

        int affectedStudents = retentionAlertService.generateForActiveStudents().size();
        return ResponseEntity.ok(buildResponse("generate-retention-alerts", affectedStudents));
    }

    private boolean isAuthorized(String headerSecret, String querySecret) {
        String secret = headerSecret != null && !headerSecret.isBlank() ? headerSecret : querySecret;
        return jobSecret != null
                && !jobSecret.isBlank()
                && jobSecret.equals(secret);
    }

    private String buildResponse(String job, int affectedStudents) {
        return "ok job=" + job +
                " affectedStudents=" + affectedStudents +
                " executedAt=" + LocalDateTime.now();
    }
}
