package com.example.myapp.features.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpstreamExecutionResult {
    private boolean success;
    private String upstreamName;
    private String endpointUrl;
    private int priorityUsed;
    private String workerTaskId;
    private String outputUrl;
    private String rawResponse;
    private String errorMessage;
    private int attemptsCount;
}
