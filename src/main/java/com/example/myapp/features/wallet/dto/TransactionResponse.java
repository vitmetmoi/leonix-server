package com.example.myapp.features.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceType;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
}
