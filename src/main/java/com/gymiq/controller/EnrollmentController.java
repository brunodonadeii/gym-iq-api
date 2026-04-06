package com.gymiq.controller;

import com.gymiq.dto.request.EnrollStudentRequest;
import com.gymiq.dto.response.EnrollmentResponse;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(request));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<EnrollmentResponse>> findByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(enrollmentService.findByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/active")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION','STUDENT')")
    public ResponseEntity<EnrollmentResponse> findActiveByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(enrollmentService.findActiveByStudent(studentId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<EnrollmentResponse> changeStatus(
            @PathVariable Integer id,
            @RequestParam EnrollmentStatus newStatus) {
        return ResponseEntity.ok(enrollmentService.changeStatus(id, newStatus));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<EnrollmentResponse> renew(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer newPlanId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.renew(id, newPlanId));
    }
}