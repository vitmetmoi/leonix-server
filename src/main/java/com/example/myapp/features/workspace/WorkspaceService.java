package com.example.myapp.features.workspace;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.workspace.dto.*;

import java.util.List;

public interface WorkspaceService {
    List<ProjectResponse> listProjects(User user);
    ProjectResponse createProject(User user, ProjectCreateRequest request);
    void deleteProject(User user, Long projectId);
    Project getProjectById(Long projectId, User user);

    List<PromptTemplateResponse> listPrompts(String category, String serviceType, String query);
    PromptTemplateResponse createPrompt(User user, PromptTemplateCreateRequest request);
    void likePrompt(Long promptId);
}
