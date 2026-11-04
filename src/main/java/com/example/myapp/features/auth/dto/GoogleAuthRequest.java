package com.example.myapp.features.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleAuthRequest {
    private String idToken;
    private String email;
    private String name;
    private String avatarUrl;
    private String turnstileToken;
}
