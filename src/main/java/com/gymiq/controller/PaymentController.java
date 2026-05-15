package com.gymiq.controller;

import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.service.PaymentService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PaymentResponse>> findAll(
            @PageableDefault(size = 10, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PaymentResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PaymentResponse>> findByEnrollment(
            @PathVariable Integer enrollmentId,
            @PageableDefault(size = 10, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.findByEnrollment(enrollmentId, pageable));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PaymentResponse>> findByStudent(
            @PathVariable Integer studentId,
            @PageableDefault(size = 10, sort = "dueDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.findByStudent(studentId, pageable));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<Page<PaymentResponse>> findOverdue(
            @PageableDefault(size = 10, sort = "dueDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.findOverdue(pageable));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PaymentResponse> pay(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) PayPaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PaymentResponse> changeStatus(
            @PathVariable Integer id,
            @RequestParam PaymentStatus newStatus) {
        return ResponseEntity.ok(paymentService.changeStatus(id, newStatus));
    }

    @PatchMapping("/refresh-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> refreshOverdue() {
        return ResponseEntity.ok(paymentService.refreshOverdue());
    }
}
