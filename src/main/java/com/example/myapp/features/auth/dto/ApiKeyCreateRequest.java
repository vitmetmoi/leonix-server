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
public class ApiKeyCreateRequest {
    @NotBlank(message = "Tên API key không được để trống")
    private String name;
}
