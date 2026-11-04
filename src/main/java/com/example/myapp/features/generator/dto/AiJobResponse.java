package com.example.myapp.features.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiJobResponse {
    private String jobUuid;
    private String serviceType;
    private String inputMode;
    private String modelId;
    private String prompt;
    private String negativePrompt;
    private String status;
    private Integer progress;
    private BigDecimal costCredits;
    private String outputMediaUrl;
    private String errorMessage;
    private Long projectId;
    private String projectName;
    private List<String> referenceMediaUrls;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
