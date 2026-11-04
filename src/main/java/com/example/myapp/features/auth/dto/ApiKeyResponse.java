package com.example.myapp.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {
    private Long id;
    private String name;
    private String keyPrefix;
    private String secretKey; // Only returned once on creation
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
}
