package com.example.myapp.features.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCapabilityDto {
    private Long id;
    private String capabilityCode;
    private String name;
    private String supportedResolutions;
    private String supportedDurations;
    private String supportedAspectRatios;
    private Long dispatchGroupId;
}
