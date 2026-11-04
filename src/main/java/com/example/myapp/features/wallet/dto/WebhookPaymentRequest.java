package com.example.myapp.features.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookPaymentRequest {
    private String gatewayTransactionId;
    private String transferContent;
    private BigDecimal amount;
    private String bankCode;
    private String signature;
}
