package com.example.myapp.features.upload.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {
    private String fileName;
    private String fileUrl;
    private String contentType;
    private long size;
}
