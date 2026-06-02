package com.gymiq.controller;

import com.gymiq.dto.response.FinancialDashboardResponse;
import com.gymiq.dto.response.OperationsDashboardResponse;
import com.gymiq.dto.response.RetentionDashboardResponse;
import com.gymiq.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/retention")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RetentionDashboardResponse> getRetentionDashboard() {
        return ResponseEntity.ok(dashboardService.getRetentionDashboard());
    }

    @GetMapping("/financial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FinancialDashboardResponse> getFinancialDashboard() {
        return ResponseEntity.ok(dashboardService.getFinancialDashboard());
    }

    @GetMapping("/operations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OperationsDashboardResponse> getOperationsDashboard() {
        return ResponseEntity.ok(dashboardService.getOperationsDashboard());
    }
}
