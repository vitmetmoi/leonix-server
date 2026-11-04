package com.example.myapp.features.auth;

import com.example.myapp.features.auth.dto.*;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register user: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đăng ký thành công", response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        log.info("REST request to login/register via Google OAuth: {}", request.getEmail());
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập Google thành công", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(user)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(authService.getDashboardStats(user)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = authService.getCurrentUser();
        authService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(authService.listApiKeys(user)));
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(@Valid @RequestBody ApiKeyCreateRequest request) {
        User user = authService.getCurrentUser();
        ApiKeyResponse response = authService.createApiKey(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo API key thành công", response));
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        authService.revokeApiKey(user, id);
        return ResponseEntity.ok(ApiResponse.success("Hủy API key thành công", null));
    }

    @GetMapping("/llm-config")
    public ResponseEntity<ApiResponse<UserLlmConfig>> getLlmConfig() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(authService.getLlmConfig(user)));
    }

    @PostMapping("/llm-config")
    public ResponseEntity<ApiResponse<UserLlmConfig>> saveLlmConfig(@Valid @RequestBody UserLlmConfigRequest request) {
        User user = authService.getCurrentUser();
        UserLlmConfig config = authService.saveLlmConfig(user, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu cấu hình LLM thành công", config));
    }
}
