package com.example.myapp.features.generator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVoiceJobRequest {
    @NotBlank(message = "Model không được để trống")
    private String model;

    @NotBlank(message = "Nội dung văn bản không được để trống")
    private String text;

    private String voiceId;

    @Builder.Default
    private String language = "vi";

    @Builder.Default
    private Double speed = 1.0;

    @Builder.Default
    private Double pitch = 1.0;

    @Builder.Default
    private Boolean exportSrt = false;

    private Long projectId;
}
