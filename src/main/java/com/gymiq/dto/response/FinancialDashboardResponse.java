package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FinancialDashboardResponse {

    private BigDecimal paidAmountCurrentMonth;
    private BigDecimal pendingAmountCurrentMonth;
    private BigDecimal overdueAmountCurrentMonth;
    private BigDecimal projectedRevenueCurrentMonth;
    private Long paidPaymentsCurrentMonth;
    private Long pendingPaymentsCurrentMonth;
    private Long overduePaymentsCurrentMonth;
    private BigDecimal defaultRate;
    private LocalDateTime generatedAt;
}
