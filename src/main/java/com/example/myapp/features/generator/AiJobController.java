package com.example.myapp.features.generator;

import com.example.myapp.features.auth.AuthService;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.generator.dto.*;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class AiJobController {

    private final AiJobService aiJobService;
    private final AuthService authService;

    @PostMapping("/video")
    public ResponseEntity<ApiResponse<AiJobResponse>> createVideoJob(@Valid @RequestBody CreateVideoJobRequest request) {
        User user = authService.getCurrentUser();
        AiJobResponse response = aiJobService.createVideoJob(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Khởi tạo tác vụ tạo video thành công", response));
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<AiJobResponse>> createImageJob(@Valid @RequestBody CreateImageJobRequest request) {
        User user = authService.getCurrentUser();
        AiJobResponse response = aiJobService.createImageJob(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Khởi tạo tác vụ tạo ảnh thành công", response));
    }

    @PostMapping("/voice")
    public ResponseEntity<ApiResponse<AiJobResponse>> createVoiceJob(@Valid @RequestBody CreateVoiceJobRequest request) {
        User user = authService.getCurrentUser();
        AiJobResponse response = aiJobService.createVoiceJob(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Khởi tạo tác vụ tạo giọng nói thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AiJobResponse>>> listJobs(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "48") int limit
    ) {
        User user = authService.getCurrentUser();
        List<AiJobResponse> jobs = aiJobService.listJobs(user, serviceType, status, limit);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AiJobResponse>> getJob(@PathVariable String uuid) {
        User user = authService.getCurrentUser();
        AiJobResponse response = aiJobService.getJob(user, uuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String uuid) {
        User user = authService.getCurrentUser();
        aiJobService.deleteJob(user, uuid);
        return ResponseEntity.ok(ApiResponse.success("Xóa tác vụ thành công", null));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Void>> batchDeleteJobs(@Valid @RequestBody BatchDeleteJobsRequest request) {
        User user = authService.getCurrentUser();
        aiJobService.batchDeleteJobs(user, request.getJobUuids());
        return ResponseEntity.ok(ApiResponse.success("Xóa hàng loạt tác vụ thành công", null));
    }
}
