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
public class CreateImageJobRequest {
    @NotBlank(message = "Model không được để trống")
    private String model;

    @Builder.Default
    private String mode = "t2i"; // t2i, i2i

    @NotBlank(message = "Prompt không được để trống")
    private String prompt;

    @Builder.Default
    private String resolution = "1k"; // 1k, 2k

    @Builder.Default
    private String aspectRatio = "1:1";

    private Long projectId;
    private List<String> referenceImages;
}
