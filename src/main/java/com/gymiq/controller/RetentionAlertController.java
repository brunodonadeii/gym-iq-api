package com.gymiq.controller;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.service.RetentionAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retention-alerts")
@RequiredArgsConstructor
public class RetentionAlertController {

    private final RetentionAlertService retentionAlertService;

    @PostMapping("/student/{studentId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RetentionAlertResponse> generateForStudent(@PathVariable Integer studentId) {
        return ResponseEntity.ok(retentionAlertService.generateForStudent(studentId));
    }

    @PostMapping("/generate-active-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RetentionAlertResponse>> generateForActiveStudents() {
        return ResponseEntity.ok(retentionAlertService.generateForActiveStudents());
    }

    @PostMapping("/generate-overdue-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RetentionAlertResponse>> generateForOverdueStudents() {
        return ResponseEntity.ok(retentionAlertService.generateForOverdueStudents());
    }

    @GetMapping("/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RetentionAlertResponse>> findOpenAlerts(
            @PageableDefault(size = 10, sort = "riskScore", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(retentionAlertService.findOpenAlerts(pageable));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RetentionAlertResponse>> findByStudent(
            @PathVariable Integer studentId,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(retentionAlertService.findByStudent(studentId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RetentionAlertResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(retentionAlertService.findById(id));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RetentionAlertResponse> resolve(@PathVariable Integer id) {
        return ResponseEntity.ok(retentionAlertService.resolve(id));
    }
}
