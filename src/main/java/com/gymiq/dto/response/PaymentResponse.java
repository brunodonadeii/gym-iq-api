package com.gymiq.dto.response;

import com.gymiq.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Integer paymentId;
    private Integer enrollmentId;

    private Integer studentId;
    private String studentName;
    private String studentEmail;

    private Integer planId;
    private String planName;

    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private String status;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse fromEntity(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .enrollmentId(p.getEnrollment().getEnrollmentId())
                .studentId(p.getEnrollment().getStudent().getStudentId())
                .studentName(p.getEnrollment().getStudent().getUser().getName())
                .studentEmail(p.getEnrollment().getStudent().getUser().getEmail())
                .planId(p.getEnrollment().getPlan().getPlanId())
                .planName(p.getEnrollment().getPlan().getName())
                .amount(p.getAmount())
                .dueDate(p.getDueDate())
                .paidAt(p.getPaidAt())
                .status(p.getStatus().name())
                .paymentMethod(p.getPaymentMethod())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
