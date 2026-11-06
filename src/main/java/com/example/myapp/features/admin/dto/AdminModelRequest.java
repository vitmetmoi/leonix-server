package com.example.myapp.features.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminModelRequest {
    @NotBlank(message = "Model ID không được để trống")
    private String id;

    @NotBlank(message = "Tên Model không được để trống")
    private String name;

    @NotBlank(message = "Nhà cung cấp không được để trống")
    private String provider;

    @NotBlank(message = "Loại dịch vụ không được để trống")
    private String serviceType; // VIDEO, IMAGE, VOICE

    private String capabilities;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isMaintenance = false;

    @Builder.Default
    private Integer displayOrder = 0;

    private Long dispatchGroupId;
}
