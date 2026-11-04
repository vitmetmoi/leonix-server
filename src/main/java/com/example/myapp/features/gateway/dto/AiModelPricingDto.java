package com.example.myapp.features.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiModelPricingDto {
    private Long id;
    private String modelId;
    private String capabilityCode;
    private String resolution;
    private String duration;
    private String priceUnit;
    private BigDecimal sellPrice;
}
