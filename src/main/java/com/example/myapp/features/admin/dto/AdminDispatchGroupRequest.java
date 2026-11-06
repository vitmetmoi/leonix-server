package com.example.myapp.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDispatchGroupRequest {
    private Long id;

    @NotBlank
    private String groupCode;

    @NotBlank
    private String name;

    private String strategy; // PRIORITY_FAILOVER, ROUND_ROBIN, WEIGHTED

    private String failoverStatusCodes;

    private Integer maxRetriesPerUpstream;

    private Integer timeoutMs;
}
