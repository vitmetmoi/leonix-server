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
public class ProjectCreateRequest {
    @NotBlank(message = "Tên dự án không được để trống")
    private String name;

    @Builder.Default
    private String kind = "STANDARD"; // STANDARD, FLOW_BOARD, SALES_VIDEO, TVC_STUDIO, STORY_ANIMATION, THREADS_CONTENT, DUBBING

    private String coverUrl;
    private String description;
}
