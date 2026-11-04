package com.example.myapp.features.wallet;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.wallet.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    private static final String DEFAULT_BANK_CODE = "TPBank";
    private static final String DEFAULT_ACCOUNT_NUMBER = "0888899999";
    private static final String DEFAULT_ACCOUNT_NAME = "LEONIX STUDIO";

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("Initializing new wallet for user ID: {}", user.getId());
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .reserved(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public WalletResponse getWalletResponse(User user) {
        Wallet wallet = getOrCreateWallet(user);
        return WalletResponse.builder()
                .id(wallet.getId())
                .balance(wallet.getBalance())
                .reserved(wallet.getReserved())
                .available(wallet.getAvailable())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse createDeposit(User user, DepositCreateRequest request) {
        Wallet wallet = getOrCreateWallet(user);
        String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        int randomSuffix = 1000 + new Random().nextInt(9000);
        String transferContent = "LN" + user.getId() + "X" + randomSuffix;

        BigDecimal amount = request.getAmountVnd();
        BigDecimal credits = amount; // 1 VND = 1 Credit

        String encodedName = URLEncoder.encode(DEFAULT_ACCOUNT_NAME, StandardCharsets.UTF_8);
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                DEFAULT_BANK_CODE, DEFAULT_ACCOUNT_NUMBER, amount.toBigInteger().toString(), encodedContent, encodedName);

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .user(user)
                .wallet(wallet)
                .amountVnd(amount)
                .credits(credits)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "VIETQR")
                .transferContent(transferContent)
                .bankCode(DEFAULT_BANK_CODE)
                .accountNumber(DEFAULT_ACCOUNT_NUMBER)
                .accountName(DEFAULT_ACCOUNT_NAME)
                .qrUrl(qrUrl)
                .status("PENDING")
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Created deposit order: paymentId={}, amount={}, transferContent={}", paymentId, amount, transferContent);

        return mapToPaymentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processWebhook(WebhookPaymentRequest request) {
        log.info("Received payment webhook: content={}, amount={}", request.getTransferContent(), request.getAmount());

        Payment payment = null;
        if (request.getTransferContent() != null) {
            payment = paymentRepository.findByTransferContent(request.getTransferContent()).orElse(null);
        }

        if (payment == null) {
            log.warn("Webhook ignored: no matching payment for content: {}", request.getTransferContent());
            throw new ResourceNotFoundException("Payment", "transferContent", request.getTransferContent());
        }

        if ("COMPLETED".equals(payment.getStatus())) {
            log.info("Payment {} already completed, skipping.", payment.getPaymentId());
            return mapToPaymentResponse(payment);
        }

        payment.setStatus("COMPLETED");
        payment.setGatewayTransactionId(request.getGatewayTransactionId());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Wallet wallet = payment.getWallet();
        BigDecimal newBalance = wallet.getBalance().add(payment.getCredits());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType("DEPOSIT")
                .amount(payment.getCredits())
                .balanceAfter(newBalance)
                .referenceType("PAYMENT")
                .referenceId(payment.getPaymentId())
                .description("Nạp tiền tự động qua VietQR: " + payment.getTransferContent())
                .build();
        walletTransactionRepository.save(tx);

        log.info("Payment {} successfully credited. User ID={}, new balance={}",
                payment.getPaymentId(), wallet.getUser().getId(), newBalance);

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public void reserveCredits(User user, BigDecimal amount, String jobUuid) {
        Wallet wallet = getOrCreateWallet(user);
        if (wallet.getAvailable().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư khả dụng không đủ để thực hiện (" +
                    wallet.getAvailable() + " < " + amount + " credits)");
        }

        wallet.setReserved(wallet.getReserved().add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType("JOB_RESERVE")
                .amount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .referenceType("AI_JOB")
                .referenceId(jobUuid)
                .description("Tạm khóa credit cho tác vụ " + jobUuid)
                .build();
        walletTransactionRepository.save(tx);
        log.info("Reserved {} credits for user {} on job {}", amount, user.getEmail(), jobUuid);
    }

    @Override
    @Transactional
    public void settleCredits(User user, BigDecimal amount, String jobUuid) {
        Wallet wallet = getOrCreateWallet(user);
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        BigDecimal newReserved = wallet.getReserved().subtract(amount);
        if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
            newReserved = BigDecimal.ZERO;
        }

        wallet.setBalance(newBalance);
        wallet.setReserved(newReserved);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType("JOB_SETTLE")
                .amount(amount.negate())
                .balanceAfter(newBalance)
                .referenceType("AI_JOB")
                .referenceId(jobUuid)
                .description("Khấu trừ credit hoàn thành tác vụ " + jobUuid)
                .build();
        walletTransactionRepository.save(tx);
        log.info("Settled {} credits for user {} on job {}", amount, user.getEmail(), jobUuid);
    }

    @Override
    @Transactional
    public void refundCredits(User user, BigDecimal amount, String jobUuid) {
        Wallet wallet = getOrCreateWallet(user);
        BigDecimal newReserved = wallet.getReserved().subtract(amount);
        if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
            newReserved = BigDecimal.ZERO;
        }

        wallet.setReserved(newReserved);
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType("JOB_REFUND")
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceType("AI_JOB")
                .referenceId(jobUuid)
                .description("Hoàn lại credit do tác vụ " + jobUuid + " thất bại")
                .build();
        walletTransactionRepository.save(tx);
        log.info("Refunded reserved {} credits for user {} on job {}", amount, user.getEmail(), jobUuid);
    }

    @Override
    @Transactional
    public List<TransactionResponse> listTransactions(User user, Pageable pageable) {
        Wallet wallet = getOrCreateWallet(user);
        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .stream()
                .map(tx -> TransactionResponse.builder()
                        .id(tx.getId())
                        .transactionType(tx.getTransactionType())
                        .amount(tx.getAmount())
                        .balanceAfter(tx.getBalanceAfter())
                        .referenceType(tx.getReferenceType())
                        .referenceId(tx.getReferenceId())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .amountVnd(payment.getAmountVnd())
                .credits(payment.getCredits())
                .paymentMethod(payment.getPaymentMethod())
                .transferContent(payment.getTransferContent())
                .bankCode(payment.getBankCode())
                .accountNumber(payment.getAccountNumber())
                .accountName(payment.getAccountName())
                .qrUrl(payment.getQrUrl())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .expiredAt(payment.getExpiredAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
