package com.example.myapp.features.library.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDeleteAssetsRequest {
    @NotEmpty(message = "Danh sách ID tài sản không được để trống")
    private List<Long> assetIds;
}
