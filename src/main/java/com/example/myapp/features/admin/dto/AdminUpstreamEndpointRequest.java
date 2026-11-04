package com.example.myapp.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpstreamEndpointRequest {
    private Long id;
    private Long dispatchGroupId;
    @NotBlank
    private String name;
    private Integer priority;
    private Integer weight;
    @NotBlank
    private String endpointUrl;
    private String httpMethod;
    private String headersTemplate;
    private String apiKey;
    private String requestBodyTemplate;
    private String responseMapping;
    private Boolean isActive;
}
