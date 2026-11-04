package com.example.myapp.features.auth;

import com.example.myapp.features.auth.dto.*;

import java.util.List;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    User register(String email, String password, String fullName, String role);
    AuthResponse register(RegisterRequest request);
    AuthResponse googleLogin(GoogleAuthRequest request);
    User getCurrentUser();
    UserProfileResponse getProfile(User user);
    DashboardStatsResponse getDashboardStats(User user);
    void changePassword(User user, ChangePasswordRequest request);
    List<ApiKeyResponse> listApiKeys(User user);
    ApiKeyResponse createApiKey(User user, ApiKeyCreateRequest request);
    void revokeApiKey(User user, Long keyId);
    UserLlmConfig getLlmConfig(User user);
    UserLlmConfig saveLlmConfig(User user, UserLlmConfigRequest request);
}
