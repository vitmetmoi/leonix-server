package com.example.myapp.features.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private BigDecimal balance;
    private BigDecimal reservedBalance;
    private Long totalJobs;
    private LocalDateTime createdAt;
}
