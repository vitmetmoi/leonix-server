package com.example.myapp.features.auth;

import com.example.myapp.config.jwt.JwtUtils;
import com.example.myapp.features.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldLoginSuccessfullyWhenCredentialsAreValid() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_pass")
                .fullName("Test User")
                .role("USER")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtUtils.generateToken(eq("test@example.com"), any())).thenReturn("mocked.jwt.token");

        var response = authService.login(new LoginRequest("test@example.com", "password123"));

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }
}
