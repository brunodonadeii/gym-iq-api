package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationsDashboardResponse {

    private Long checkInsToday;
    private Long openCheckIns;
    private Long activeEnrollments;
    private Long suspendedEnrollments;
    private Long canceledEnrollments;
    private Long enrollmentsExpiringInNext7Days;
    private Long newStudentsCurrentMonth;
    private LocalDateTime generatedAt;
}
