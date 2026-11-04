package com.example.myapp.features.wallet;

import com.example.myapp.features.auth.AuthService;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.wallet.dto.*;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet() {
        User user = authService.getCurrentUser();
        WalletResponse response = walletService.getWalletResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<PaymentResponse>> createDeposit(@Valid @RequestBody DepositCreateRequest request) {
        User user = authService.getCurrentUser();
        PaymentResponse response = walletService.createDeposit(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo mã nạp tiền thành công", response));
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = walletService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authService.getCurrentUser();
        List<TransactionResponse> transactions = walletService.listTransactions(user, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentResponse>> handleWebhook(@RequestBody WebhookPaymentRequest request) {
        log.info("Processing incoming payment webhook callback");
        PaymentResponse response = walletService.processWebhook(request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý webhook thành công", response));
    }
}
