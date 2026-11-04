package com.example.myapp.features.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewResponse {
    private Long totalUsers;
    private Long totalJobs;
    private BigDecimal totalRevenueVnd;
    private Long activeUpstreamsCount;
    private Long healthyUpstreamsCount;
    private Map<String, Long> jobsByService;
    private Map<String, Long> jobsByStatus;
    private List<AdminJobResponse> recentJobs;
    private List<AdminPaymentResponse> recentPayments;
}
