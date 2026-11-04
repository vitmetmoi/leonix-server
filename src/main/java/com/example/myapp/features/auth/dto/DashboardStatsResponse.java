package com.example.myapp.features.auth.dto;

import com.example.myapp.features.generator.dto.AiJobResponse;
import com.example.myapp.features.wallet.dto.WalletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Map<String, Long> jobsByStatus;
    private Long totalJobs;
    private Long completedCredits;
    private WalletResponse wallet;
    private List<AiJobResponse> recentJobs;
}
