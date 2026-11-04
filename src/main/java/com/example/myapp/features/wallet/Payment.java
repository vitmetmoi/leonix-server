package com.example.myapp.features.wallet;

import com.example.myapp.features.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 64)
    private String paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "amount_vnd", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountVnd;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal credits;

    @Builder.Default
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "VIETQR";

    @Column(name = "transfer_content", nullable = false, unique = true, length = 50)
    private String transferContent;

    @Builder.Default
    @Column(name = "bank_code", length = 20)
    private String bankCode = "TPBank";

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 100)
    private String accountName;

    @Column(name = "qr_url", columnDefinition = "TEXT")
    private String qrUrl;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, COMPLETED, EXPIRED, CANCELLED

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
