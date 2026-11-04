package com.example.myapp.features.wallet;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.wallet.dto.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    Wallet getOrCreateWallet(User user);
    WalletResponse getWalletResponse(User user);
    PaymentResponse createDeposit(User user, DepositCreateRequest request);
    PaymentResponse getPayment(String paymentId);
    PaymentResponse processWebhook(WebhookPaymentRequest request);
    void reserveCredits(User user, BigDecimal amount, String jobUuid);
    void settleCredits(User user, BigDecimal amount, String jobUuid);
    void refundCredits(User user, BigDecimal amount, String jobUuid);
    List<TransactionResponse> listTransactions(User user, Pageable pageable);
}
