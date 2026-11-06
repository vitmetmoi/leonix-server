package com.example.myapp.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPricingRequest {
    private Long id;

    @NotBlank(message = "Model ID không được để trống")
    private String modelId;

    @NotBlank(message = "Mã tính năng (capabilityCode) không được để trống")
    private String capabilityCode;

    private String resolution;
    private String duration;

    @Builder.Default
    private String priceUnit = "PER_CALL";

    @NotNull(message = "Giá bán không được để trống")
    private BigDecimal sellPrice;

    private BigDecimal costPrice;
}
