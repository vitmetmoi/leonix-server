package com.example.myapp.features.generator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVideoJobRequest {
    @NotBlank(message = "Model không được để trống")
    private String model;

    @Builder.Default
    private String mode = "t2v"; // t2v, i2v, r2v, startend

    @NotBlank(message = "Prompt không được để trống")
    private String prompt;

    private String negativePrompt;

    @Builder.Default
    private String duration = "8s";

    @Builder.Default
    private String resolution = "720p";

    @Builder.Default
    private String aspectRatio = "16:9";

    private Long projectId;
    private List<String> referenceImages;
}
