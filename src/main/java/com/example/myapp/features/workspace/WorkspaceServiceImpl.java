package com.example.myapp.features.workspace;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.workspace.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    private final ProjectRepository projectRepository;
    private final PromptTemplateRepository promptTemplateRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(User user) {
        return projectRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse createProject(User user, ProjectCreateRequest request) {
        Project project = Project.builder()
                .user(user)
                .name(request.getName().trim())
                .kind(request.getKind() != null ? request.getKind() : "STANDARD")
                .coverUrl(request.getCoverUrl())
                .description(request.getDescription())
                .build();

        Project saved = projectRepository.save(project);
        log.info("Created project '{}' (id: {}) for user: {}", saved.getName(), saved.getId(), user.getEmail());
        return mapToProjectResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProject(User user, Long projectId) {
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        projectRepository.delete(project);
        log.info("Deleted project id: {} by user: {}", projectId, user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectById(Long projectId, User user) {
        if (projectId == null) return null;
        return projectRepository.findByIdAndUserId(projectId, user.getId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptTemplateResponse> listPrompts(String category, String serviceType, String query) {
        return promptTemplateRepository.searchPrompts(category, serviceType, query)
                .stream()
                .map(this::mapToPromptResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromptTemplateResponse createPrompt(User user, PromptTemplateCreateRequest request) {
        PromptTemplate template = PromptTemplate.builder()
                .title(request.getTitle().trim())
                .category(request.getCategory().toUpperCase())
                .serviceType(request.getServiceType().toUpperCase())
                .promptVi(request.getPromptVi())
                .promptEn(request.getPromptEn())
                .previewMediaUrl(request.getPreviewMediaUrl())
                .tags(request.getTags())
                .isSystem(false)
                .user(user)
                .likesCount(0)
                .build();

        PromptTemplate saved = promptTemplateRepository.save(template);
        log.info("Created prompt template '{}' (id: {}) by user: {}", saved.getTitle(), saved.getId(), user.getEmail());
        return mapToPromptResponse(saved);
    }

    @Override
    @Transactional
    public void likePrompt(Long promptId) {
        PromptTemplate template = promptTemplateRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", "id", promptId));
        template.setLikesCount(template.getLikesCount() + 1);
        promptTemplateRepository.save(template);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .kind(project.getKind())
                .coverUrl(project.getCoverUrl())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private PromptTemplateResponse mapToPromptResponse(PromptTemplate prompt) {
        return PromptTemplateResponse.builder()
                .id(prompt.getId())
                .title(prompt.getTitle())
                .category(prompt.getCategory())
                .serviceType(prompt.getServiceType())
                .promptVi(prompt.getPromptVi())
                .promptEn(prompt.getPromptEn())
                .previewMediaUrl(prompt.getPreviewMediaUrl())
                .tags(prompt.getTags())
                .isSystem(prompt.getIsSystem())
                .likesCount(prompt.getLikesCount())
                .createdAt(prompt.getCreatedAt())
                .build();
    }
}
