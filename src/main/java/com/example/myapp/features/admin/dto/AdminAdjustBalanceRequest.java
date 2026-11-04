package com.example.myapp.features.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAdjustBalanceRequest {
    @NotNull(message = "Amount cannot be null")
    private BigDecimal amount;
    private String reason;
}
