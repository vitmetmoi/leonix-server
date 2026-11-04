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
public class AdminJobResponse {
    private Long id;
    private String jobUuid;
    private String userEmail;
    private String serviceType;
    private String model;
    private String status;
    private Integer progress;
    private BigDecimal costCredits;
    private String prompt;
    private String outputUrl;
    private String errorMessage;
    private String executionNode;
    private LocalDateTime createdAt;
}
