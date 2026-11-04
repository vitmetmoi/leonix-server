package com.example.myapp.features.workspace;

import com.example.myapp.features.auth.AuthService;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.workspace.dto.*;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final AuthService authService;

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listProjects() {
        User user = authService.getCurrentUser();
        List<ProjectResponse> projects = workspaceService.listProjects(user);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        User user = authService.getCurrentUser();
        ProjectResponse response = workspaceService.createProject(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo dự án thành công", response));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        workspaceService.deleteProject(user, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa dự án thành công", null));
    }

    @GetMapping("/prompts")
    public ResponseEntity<ApiResponse<List<PromptTemplateResponse>>> listPrompts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String query
    ) {
        List<PromptTemplateResponse> prompts = workspaceService.listPrompts(category, serviceType, query);
        return ResponseEntity.ok(ApiResponse.success(prompts));
    }

    @PostMapping("/prompts")
    public ResponseEntity<ApiResponse<PromptTemplateResponse>> createPrompt(@Valid @RequestBody PromptTemplateCreateRequest request) {
        User user = authService.getCurrentUser();
        PromptTemplateResponse response = workspaceService.createPrompt(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Lưu prompt thành công", response));
    }

    @PostMapping("/prompts/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likePrompt(@PathVariable Long id) {
        workspaceService.likePrompt(id);
        return ResponseEntity.ok(ApiResponse.success("Đã thích prompt", null));
    }
}
