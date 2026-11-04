package com.example.myapp.features.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositCreateRequest {

    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền nạp tối thiểu là 10.000 VNĐ")
    private BigDecimal amountVnd;

    @Builder.Default
    private String paymentMethod = "VIETQR"; // VIETQR or USDT
}
