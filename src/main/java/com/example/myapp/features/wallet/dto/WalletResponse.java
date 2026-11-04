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
public class WalletResponse {
    private Long id;
    private BigDecimal balance;
    private BigDecimal reserved;
    private BigDecimal available;
    private LocalDateTime updatedAt;
}
