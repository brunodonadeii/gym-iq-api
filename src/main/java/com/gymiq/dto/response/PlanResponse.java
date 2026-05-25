package com.gymiq.dto.response;

import com.gymiq.entity.Plan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlanResponse {

    private Integer planId;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private Integer durationMonths;
    private Boolean active;
    private LocalDateTime createdAt;

    public static PlanResponse fromEntity(Plan plan) {
        return PlanResponse.builder()
                .planId(plan.getPlanId())
                .name(plan.getName())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .durationMonths(plan.getDurationMonths())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
