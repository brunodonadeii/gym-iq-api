package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentJobResponse {

    private String job;
    private Integer affectedPayments;
    private LocalDateTime executedAt;
}
