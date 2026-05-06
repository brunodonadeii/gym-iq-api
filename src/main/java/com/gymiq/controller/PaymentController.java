package com.gymiq.controller;

import com.gymiq.dto.request.CreatePaymentRequest;
import com.gymiq.dto.request.PayPaymentRequest;
import com.gymiq.dto.response.PaymentResponse;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> findAll() {
        return ResponseEntity.ok(paymentService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<PaymentResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> findByEnrollment(
            @PathVariable Integer enrollmentId) {
        return ResponseEntity.ok(paymentService.findByEnrollment(enrollmentId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> findByStudent(@PathVariable Integer studentId) {
        return ResponseEntity.ok(paymentService.findByStudent(studentId));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> findOverdue() {
        return ResponseEntity.ok(paymentService.findOverdue());
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

    @PostMapping("/refresh-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTION')")
    public ResponseEntity<List<PaymentResponse>> refreshOverdue() {
        return ResponseEntity.ok(paymentService.refreshOverdue());
    }
}
