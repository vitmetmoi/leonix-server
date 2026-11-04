package com.example.myapp.features.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplateResponse {
    private Long id;
    private String title;
    private String category;
    private String serviceType;
    private String promptVi;
    private String promptEn;
    private String previewMediaUrl;
    private String tags;
    private Boolean isSystem;
    private Integer likesCount;
    private LocalDateTime createdAt;
}
