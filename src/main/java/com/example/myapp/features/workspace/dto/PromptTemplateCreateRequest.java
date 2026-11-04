package com.example.myapp.features.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplateCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Danh mục không được để trống")
    private String category; // CINEMATIC, TIKTOK_REELS, SHOPEE_ADS, FASHION, ANIME

    @NotBlank(message = "Loại dịch vụ không được để trống")
    private String serviceType; // VIDEO, IMAGE

    @NotBlank(message = "Nội dung prompt tiếng Việt không được để trống")
    private String promptVi;

    private String promptEn;
    private String previewMediaUrl;
    private String tags;
}
