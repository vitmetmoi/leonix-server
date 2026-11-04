package com.example.myapp.features.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAssetResponse {
    private Long id;
    private String mediaType;
    private String url;
    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private Boolean isFavorite;
    private String metadata;
    private Long projectId;
    private String projectName;
    private String jobUuid;
    private LocalDateTime createdAt;
}
