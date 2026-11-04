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
public class PaymentResponse {
    private String paymentId;
    private BigDecimal amountVnd;
    private BigDecimal credits;
    private String paymentMethod;
    private String transferContent;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private String qrUrl;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
