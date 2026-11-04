package com.example.myapp.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLlmConfigRequest {
    private String displayName;

    @NotBlank(message = "Base URL không được để trống")
    private String baseUrl;

    @NotBlank(message = "API key không được để trống")
    private String apiKey;

    @NotBlank(message = "Model name không được để trống")
    private String modelName;

    private Boolean isActive;
}
