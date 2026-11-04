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
public class ProjectResponse {
    private Long id;
    private String name;
    private String kind;
    private String coverUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
