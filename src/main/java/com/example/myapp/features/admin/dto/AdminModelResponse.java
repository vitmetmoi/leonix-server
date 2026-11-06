package com.example.myapp.features.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminModelResponse {
    private String id;
    private String name;
    private String provider;
    private String serviceType;
    private String capabilities;
    private Boolean isActive;
    private Boolean isMaintenance;
    private Integer displayOrder;
    private Long dispatchGroupId;
    private String dispatchGroupCode;
    private String dispatchGroupName;
    private Integer pricingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
