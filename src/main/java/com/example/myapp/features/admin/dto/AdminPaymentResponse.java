package com.example.myapp.features.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {
    private Long id;
    private String paymentId;
    private String userEmail;
    private BigDecimal amountVnd;
    private BigDecimal credits;
    private String paymentMethod;
    private String transferContent;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
