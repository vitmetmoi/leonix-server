package com.example.myapp.features.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiModelDto {
    private String id;
    private String name;
    private String provider;
    private String serviceType;
    private String capabilities;
    private Boolean isMaintenance;
    private List<AiCapabilityDto> capabilitiesList;
}
