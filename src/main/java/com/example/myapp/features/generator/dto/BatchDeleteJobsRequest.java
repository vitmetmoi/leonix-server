package com.example.myapp.features.generator.dto;

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
public class BatchDeleteJobsRequest {
    @NotEmpty(message = "Danh sách job id không được để trống")
    private List<String> jobUuids;
}
