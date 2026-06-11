package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OperationsDashboardResponse {

    private Long checkInsToday;
    private Long activeEnrollments;
    private Long suspendedEnrollments;
    private Long canceledEnrollments;
    private Long enrollmentsExpiringInNext7Days;
    private Long newStudentsCurrentMonth;
    private Long activeCustomersAtPeriodStart;
    private Long lostCustomersInPeriod;
    private BigDecimal churnRate;
    private LocalDateTime generatedAt;
}
