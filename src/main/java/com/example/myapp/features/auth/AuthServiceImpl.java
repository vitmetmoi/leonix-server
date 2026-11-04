package com.example.myapp.features.auth;

import com.example.myapp.config.jwt.JwtUtils;
import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.auth.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.example.myapp.features.generator.AiJobRepository;
import com.example.myapp.features.generator.AiJobService;
import com.example.myapp.features.generator.dto.AiJobResponse;
import com.example.myapp.features.mail.EmailService;
import com.example.myapp.features.wallet.WalletService;
import com.example.myapp.features.wallet.dto.WalletResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final UserLlmConfigRepository userLlmConfigRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AiJobRepository aiJobRepository;
    private final AiJobService aiJobService;
    private final WalletService walletService;
    private final TurnstileService turnstileService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        if (request.getTurnstileToken() != null && !request.getTurnstileToken().isBlank()) {
            boolean valid = turnstileService.verify(request.getTurnstileToken(), null);
            if (!valid) {
                log.warn("Login rejected: Turnstile verification failed for email: {}", request.getEmail());
                throw new BadCredentialsException("Xác thực bảo mật Cloudflare Turnstile không thành công");
            }
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user with email '{}' not found", request.getEmail());
                    return new ResourceNotFoundException("User", "email", request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: bad credentials for email '{}'", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("fullName", user.getFullName());

        String token = jwtUtils.generateToken(user.getEmail(), claims);
        log.info("User '{}' successfully logged in. JWT token generated.", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Override
    @Transactional
    public User register(String email, String password, String fullName, String role) {
        log.info("Registration request for email: {}", email);
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: email '{}' already in use", email);
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role != null ? role : "USER")
                .status("ACTIVE")
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully with ID: {}", saved.getId());

        // Initialize wallet
        walletService.getOrCreateWallet(saved);

        // Send welcome email via SMTP
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());

        return saved;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());

        if (request.getTurnstileToken() != null && !request.getTurnstileToken().isBlank()) {
            boolean valid = turnstileService.verify(request.getTurnstileToken(), null);
            if (!valid) {
                log.warn("Registration rejected: Turnstile verification failed for email: {}", request.getEmail());
                throw new BadCredentialsException("Xác thực bảo mật Cloudflare Turnstile không thành công");
            }
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: email '{}' already in use", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }

        String displayName = request.getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = request.getEmail().split("@")[0];
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(displayName)
                .role("USER")
                .status("ACTIVE")
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully with ID: {}", saved.getId());

        // Initialize wallet
        walletService.getOrCreateWallet(saved);

        // Send welcome email via SMTP
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", saved.getId());
        claims.put("role", saved.getRole());
        claims.put("fullName", saved.getFullName());

        String token = jwtUtils.generateToken(saved.getEmail(), claims);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole())
                .avatarUrl(saved.getAvatarUrl())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        log.info("Google OAuth login request received");

        String email = request.getEmail();
        String name = request.getName();
        String avatar = request.getAvatarUrl();

        // Extract claims from Google ID Token if present
        if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
            try {
                String[] parts = request.getIdToken().split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                    JsonNode node = objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
                    if ((email == null || email.isBlank()) && node.has("email")) {
                        email = node.get("email").asText();
                    }
                    if ((name == null || name.isBlank()) && node.has("name")) {
                        name = node.get("name").asText();
                    }
                    if ((avatar == null || avatar.isBlank() || avatar.contains("default-user")) && node.has("picture")) {
                        avatar = node.get("picture").asText();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not decode Google ID token payload: {}", e.getMessage());
            }
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email Google không hợp lệ hoặc không trích xuất được từ token");
        }

        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        if (avatar == null || avatar.isBlank()) {
            avatar = "https://lh3.googleusercontent.com/a/default-user";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("First-time Google sign-in. Creating new user for: {}", email);
            user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name)
                    .avatarUrl(avatar)
                    .role("USER")
                    .status("ACTIVE")
                    .build();
            user = userRepository.save(user);
            walletService.getOrCreateWallet(user);
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        } else {
            if (avatar != null && !avatar.isBlank() && !avatar.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatar);
                userRepository.save(user);
            }
            walletService.getOrCreateWallet(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("fullName", user.getFullName());

        String token = jwtUtils.generateToken(user.getEmail(), claims);
        log.info("User '{}' successfully authenticated via Google. JWT generated.", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BadCredentialsException("User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không chính xác");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    @Override
    public List<ApiKeyResponse> listApiKeys(User user) {
        return userApiKeyRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(key -> ApiKeyResponse.builder()
                        .id(key.getId())
                        .name(key.getName())
                        .keyPrefix(key.getKeyPrefix())
                        .lastUsedAt(key.getLastUsedAt())
                        .createdAt(key.getCreatedAt())
                        .revokedAt(key.getRevokedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiKeyResponse createApiKey(User user, ApiKeyCreateRequest request) {
        long activeCount = userApiKeyRepository.countByUserIdAndRevokedAtIsNull(user.getId());
        if (activeCount >= 5) {
            throw new IllegalArgumentException("Đã đạt giới hạn tối đa 5 API key đang hoạt động");
        }

        String rawSecret = "leonix_sk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String prefix = rawSecret.substring(0, 14) + "…";
        String hashed = hashSha256(rawSecret);

        UserApiKey apiKey = UserApiKey.builder()
                .user(user)
                .name(request.getName().trim())
                .keyPrefix(prefix)
                .hashedKey(hashed)
                .build();

        UserApiKey saved = userApiKeyRepository.save(apiKey);
        log.info("Created new API key '{}' for user: {}", saved.getName(), user.getEmail());

        return ApiKeyResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .keyPrefix(saved.getKeyPrefix())
                .secretKey(rawSecret) // Return once
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void revokeApiKey(User user, Long keyId) {
        UserApiKey key = userApiKeyRepository.findByIdAndUserId(keyId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", "id", keyId));
        key.setRevokedAt(LocalDateTime.now());
        userApiKeyRepository.save(key);
        log.info("Revoked API key ID: {} for user: {}", keyId, user.getEmail());
    }

    @Override
    public UserLlmConfig getLlmConfig(User user) {
        return userLlmConfigRepository.findByUserId(user.getId()).orElse(null);
    }

    @Override
    @Transactional
    public UserLlmConfig saveLlmConfig(User user, UserLlmConfigRequest request) {
        UserLlmConfig config = userLlmConfigRepository.findByUserId(user.getId())
                .orElse(UserLlmConfig.builder().user(user).build());

        config.setDisplayName(request.getDisplayName());
        config.setBaseUrl(request.getBaseUrl());
        config.setApiKeyEncrypted(request.getApiKey());
        config.setModelName(request.getModelName());
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return userLlmConfigRepository.save(config);
    }

    @Override
    @Transactional
    public DashboardStatsResponse getDashboardStats(User user) {
        log.info("Fetching dashboard stats for user ID: {}", user.getId());
        long queued = aiJobRepository.countByUserIdAndStatus(user.getId(), "QUEUED");
        long processing = aiJobRepository.countByUserIdAndStatus(user.getId(), "PROCESSING");
        long completed = aiJobRepository.countByUserIdAndStatus(user.getId(), "COMPLETED");
        long failed = aiJobRepository.countByUserIdAndStatus(user.getId(), "FAILED");
        long totalJobs = aiJobRepository.countByUserId(user.getId());
        Long completedCredits = aiJobRepository.sumCompletedCreditsByUserId(user.getId());
        if (completedCredits == null) {
            completedCredits = 0L;
        }

        Map<String, Long> statusMap = new LinkedHashMap<>();
        statusMap.put("QUEUED", queued);
        statusMap.put("PROCESSING", processing);
        statusMap.put("COMPLETED", completed);
        statusMap.put("FAILED", failed);

        List<AiJobResponse> recentJobs = aiJobService.listJobs(user, null, null, 10);
        WalletResponse walletResponse = walletService.getWalletResponse(user);

        return DashboardStatsResponse.builder()
                .jobsByStatus(statusMap)
                .totalJobs(totalJobs)
                .completedCredits(completedCredits)
                .wallet(walletResponse)
                .recentJobs(recentJobs)
                .build();
    }

    private String hashSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
