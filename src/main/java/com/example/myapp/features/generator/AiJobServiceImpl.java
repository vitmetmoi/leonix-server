package com.example.myapp.features.generator;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.gateway.AiGatewayService;
import com.example.myapp.features.gateway.AiModel;
import com.example.myapp.features.gateway.AiModelRepository;
import com.example.myapp.features.gateway.dto.UpstreamExecutionResult;
import com.example.myapp.features.generator.dto.*;
import com.example.myapp.features.library.MediaAsset;
import com.example.myapp.features.library.MediaAssetRepository;
import com.example.myapp.features.wallet.WalletService;
import com.example.myapp.features.workspace.Project;
import com.example.myapp.features.workspace.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobServiceImpl implements AiJobService {

    private final AiJobRepository aiJobRepository;
    private final JobReferenceMediaRepository jobReferenceMediaRepository;
    private final AiModelRepository aiModelRepository;
    private final ProjectRepository projectRepository;
    private final WalletService walletService;
    private final AiGatewayService aiGatewayService;
    private final MediaAssetRepository mediaAssetRepository;

    @Override
    @Transactional
    public AiJobResponse createVideoJob(User user, CreateVideoJobRequest request) {
        String jobUuid = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Creating video job {} for user {}", jobUuid, user.getEmail());

        AiModel model = getOrRegisterModel(request.getModel(), "VIDEO", "xai");
        Project project = getProjectIfPresent(request.getProjectId(), user);

        BigDecimal cost = aiGatewayService.calculatePrice(
                model.getId(), request.getMode(), request.getResolution(), request.getDuration(), null);

        // Reserve credit before execution
        walletService.reserveCredits(user, cost, jobUuid);

        AiJob job = AiJob.builder()
                .jobUuid(jobUuid)
                .user(user)
                .project(project)
                .model(model)
                .serviceType("VIDEO")
                .inputMode(request.getMode())
                .prompt(request.getPrompt())
                .negativePrompt(request.getNegativePrompt())
                .params(String.format("{\"duration\":\"%s\",\"resolution\":\"%s\",\"aspectRatio\":\"%s\"}",
                        request.getDuration(), request.getResolution(), request.getAspectRatio()))
                .status("PROCESSING")
                .progress(25)
                .costCredits(cost)
                .build();

        AiJob savedJob = aiJobRepository.save(job);

        // Save reference media if any
        if (request.getReferenceImages() != null) {
            for (String refUrl : request.getReferenceImages()) {
                JobReferenceMedia refMedia = JobReferenceMedia.builder()
                        .job(savedJob)
                        .mediaType("STYLE_REFERENCE")
                        .fileUrl(refUrl)
                        .build();
                jobReferenceMediaRepository.save(refMedia);
            }
        }

        // Dispatch to AI gateway with fallback cURL chain
        Map<String, Object> dispatchParams = new HashMap<>();
        dispatchParams.put("prompt", request.getPrompt());
        dispatchParams.put("mode", request.getMode());
        dispatchParams.put("duration", request.getDuration());
        dispatchParams.put("resolution", request.getResolution());
        dispatchParams.put("aspectRatio", request.getAspectRatio());
        if (request.getReferenceImages() != null && !request.getReferenceImages().isEmpty()) {
            dispatchParams.put("referenceImageUrl", request.getReferenceImages().get(0));
        }

        UpstreamExecutionResult dispatchResult = aiGatewayService.dispatchWithFallback(
                model.getId(), request.getMode(), dispatchParams);

        if (!dispatchResult.isSuccess()) {
            log.error("Video generation failed for job {}: {}", jobUuid, dispatchResult.getErrorMessage());
            savedJob.setStatus("FAILED");
            savedJob.setErrorMessage(dispatchResult.getErrorMessage());
            aiJobRepository.save(savedJob);

            // Auto-refund reserved credits
            walletService.refundCredits(user, cost, jobUuid);
            return mapToJobResponse(savedJob);
        }

        // Job succeeded
        String outputUrl = (dispatchResult.getOutputUrl() != null && !dispatchResult.getOutputUrl().isBlank())
                ? dispatchResult.getOutputUrl()
                : "/uploads/media/video_" + jobUuid + ".mp4";

        savedJob.setStatus("COMPLETED");
        savedJob.setProgress(100);
        savedJob.setOutputMediaUrl(outputUrl);
        savedJob.setWorkerTaskId(dispatchResult.getWorkerTaskId());
        savedJob.setFinishedAt(LocalDateTime.now());
        aiJobRepository.save(savedJob);

        // Commit/Settle wallet credits
        walletService.settleCredits(user, cost, jobUuid);

        // Create asset in media library
        createMediaAsset(user, project, savedJob, "VIDEO", outputUrl, request.getPrompt());

        return mapToJobResponse(savedJob);
    }

    @Override
    @Transactional
    public AiJobResponse createImageJob(User user, CreateImageJobRequest request) {
        String jobUuid = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Creating image job {} for user {}", jobUuid, user.getEmail());

        AiModel model = getOrRegisterModel(request.getModel(), "IMAGE", "google");
        Project project = getProjectIfPresent(request.getProjectId(), user);

        BigDecimal cost = aiGatewayService.calculatePrice(
                model.getId(), request.getMode(), request.getResolution(), null, null);

        // Reserve credit
        walletService.reserveCredits(user, cost, jobUuid);

        AiJob job = AiJob.builder()
                .jobUuid(jobUuid)
                .user(user)
                .project(project)
                .model(model)
                .serviceType("IMAGE")
                .inputMode(request.getMode())
                .prompt(request.getPrompt())
                .params(String.format("{\"resolution\":\"%s\",\"aspectRatio\":\"%s\"}",
                        request.getResolution(), request.getAspectRatio()))
                .status("PROCESSING")
                .progress(50)
                .costCredits(cost)
                .build();

        AiJob savedJob = aiJobRepository.save(job);

        Map<String, Object> dispatchParams = new HashMap<>();
        dispatchParams.put("prompt", request.getPrompt());
        dispatchParams.put("resolution", request.getResolution());
        dispatchParams.put("aspectRatio", request.getAspectRatio());

        UpstreamExecutionResult dispatchResult = aiGatewayService.dispatchWithFallback(
                model.getId(), request.getMode(), dispatchParams);

        if (!dispatchResult.isSuccess()) {
            savedJob.setStatus("FAILED");
            savedJob.setErrorMessage(dispatchResult.getErrorMessage());
            aiJobRepository.save(savedJob);
            walletService.refundCredits(user, cost, jobUuid);
            return mapToJobResponse(savedJob);
        }

        String outputUrl = (dispatchResult.getOutputUrl() != null && !dispatchResult.getOutputUrl().isBlank())
                ? dispatchResult.getOutputUrl()
                : "/uploads/media/img_" + jobUuid + ".png";

        savedJob.setStatus("COMPLETED");
        savedJob.setProgress(100);
        savedJob.setOutputMediaUrl(outputUrl);
        savedJob.setWorkerTaskId(dispatchResult.getWorkerTaskId());
        savedJob.setFinishedAt(LocalDateTime.now());
        aiJobRepository.save(savedJob);

        walletService.settleCredits(user, cost, jobUuid);
        createMediaAsset(user, project, savedJob, "IMAGE", outputUrl, request.getPrompt());

        return mapToJobResponse(savedJob);
    }

    @Override
    @Transactional
    public AiJobResponse createVoiceJob(User user, CreateVoiceJobRequest request) {
        String jobUuid = "job_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("Creating voice job {} for user {}", jobUuid, user.getEmail());

        AiModel model = getOrRegisterModel(request.getModel(), "VOICE", "elevenlabs");
        Project project = getProjectIfPresent(request.getProjectId(), user);

        int charCount = request.getText() != null ? request.getText().length() : 0;
        BigDecimal baseCost = aiGatewayService.calculatePrice(model.getId(), "tts", null, null, charCount);
        if (Boolean.TRUE.equals(request.getExportSrt())) {
            baseCost = baseCost.multiply(BigDecimal.valueOf(1.15)); // +15% surcharge for SRT
        }

        walletService.reserveCredits(user, baseCost, jobUuid);

        AiJob job = AiJob.builder()
                .jobUuid(jobUuid)
                .user(user)
                .project(project)
                .model(model)
                .serviceType("VOICE")
                .inputMode("tts")
                .prompt(request.getText())
                .params(String.format("{\"voiceId\":\"%s\",\"language\":\"%s\",\"speed\":%s,\"exportSrt\":%s}",
                        request.getVoiceId(), request.getLanguage(), request.getSpeed(), request.getExportSrt()))
                .status("PROCESSING")
                .progress(50)
                .costCredits(baseCost)
                .build();

        AiJob savedJob = aiJobRepository.save(job);

        Map<String, Object> dispatchParams = new HashMap<>();
        dispatchParams.put("text", request.getText());
        dispatchParams.put("voiceId", request.getVoiceId());
        dispatchParams.put("language", request.getLanguage());

        UpstreamExecutionResult dispatchResult = aiGatewayService.dispatchWithFallback(
                model.getId(), "tts", dispatchParams);

        if (!dispatchResult.isSuccess()) {
            savedJob.setStatus("FAILED");
            savedJob.setErrorMessage(dispatchResult.getErrorMessage());
            aiJobRepository.save(savedJob);
            walletService.refundCredits(user, baseCost, jobUuid);
            return mapToJobResponse(savedJob);
        }

        String outputUrl = (dispatchResult.getOutputUrl() != null && !dispatchResult.getOutputUrl().isBlank())
                ? dispatchResult.getOutputUrl()
                : "/uploads/media/audio_" + jobUuid + ".mp3";

        savedJob.setStatus("COMPLETED");
        savedJob.setProgress(100);
        savedJob.setOutputMediaUrl(outputUrl);
        savedJob.setWorkerTaskId(dispatchResult.getWorkerTaskId());
        savedJob.setFinishedAt(LocalDateTime.now());
        aiJobRepository.save(savedJob);

        walletService.settleCredits(user, baseCost, jobUuid);
        createMediaAsset(user, project, savedJob, "AUDIO", outputUrl, request.getText());

        return mapToJobResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiJobResponse> listJobs(User user, String serviceType, String status, int limit) {
        int pageSize = (limit > 0 && limit <= 200) ? limit : 48;
        return aiJobRepository.findJobs(user.getId(), serviceType, status, PageRequest.of(0, pageSize))
                .stream()
                .map(this::mapToJobResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AiJobResponse getJob(User user, String jobUuid) {
        AiJob job = aiJobRepository.findByJobUuidAndUserId(jobUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", "jobUuid", jobUuid));
        return mapToJobResponse(job);
    }

    @Override
    @Transactional
    public void deleteJob(User user, String jobUuid) {
        AiJob job = aiJobRepository.findByJobUuidAndUserId(jobUuid, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", "jobUuid", jobUuid));
        aiJobRepository.delete(job);
        log.info("Deleted job {} by user {}", jobUuid, user.getEmail());
    }

    @Override
    @Transactional
    public void batchDeleteJobs(User user, List<String> jobUuids) {
        if (jobUuids == null || jobUuids.isEmpty()) return;
        List<AiJob> jobs = aiJobRepository.findByJobUuidInAndUserId(jobUuids, user.getId());
        aiJobRepository.deleteAll(jobs);
        log.info("Batch deleted {} jobs for user {}", jobs.size(), user.getEmail());
    }

    private void createMediaAsset(User user, Project project, AiJob job, String mediaType, String url, String promptSnippet) {
        String snippet = promptSnippet != null && promptSnippet.length() > 60
                ? promptSnippet.substring(0, 57) + "..."
                : promptSnippet;

        MediaAsset asset = MediaAsset.builder()
                .user(user)
                .project(project)
                .job(job)
                .mediaType(mediaType)
                .url(url)
                .fileName(job.getJobUuid() + ("VIDEO".equals(mediaType) ? ".mp4" : "IMAGE".equals(mediaType) ? ".png" : ".mp3"))
                .isFavorite(false)
                .metadata(String.format("{\"promptSnippet\":\"%s\"}", snippet != null ? snippet.replace("\"", "\\\"") : ""))
                .build();

        mediaAssetRepository.save(asset);
    }

    private AiModel getOrRegisterModel(String modelId, String serviceType, String defaultProvider) {
        return aiModelRepository.findById(modelId).orElseGet(() -> {
            log.info("Registering runtime AI model catalog entry: {}", modelId);
            AiModel newModel = AiModel.builder()
                    .id(modelId)
                    .name(modelId)
                    .provider(defaultProvider)
                    .serviceType(serviceType)
                    .isActive(true)
                    .isMaintenance(false)
                    .build();
            return aiModelRepository.save(newModel);
        });
    }

    private Project getProjectIfPresent(Long projectId, User user) {
        if (projectId == null) return null;
        return projectRepository.findByIdAndUserId(projectId, user.getId()).orElse(null);
    }

    private AiJobResponse mapToJobResponse(AiJob job) {
        List<String> refUrls = (job.getReferenceMediaList() != null)
                ? job.getReferenceMediaList().stream().map(JobReferenceMedia::getFileUrl).collect(Collectors.toList())
                : Collections.emptyList();

        return AiJobResponse.builder()
                .jobUuid(job.getJobUuid())
                .serviceType(job.getServiceType())
                .inputMode(job.getInputMode())
                .modelId(job.getModel() != null ? job.getModel().getId() : null)
                .prompt(job.getPrompt())
                .negativePrompt(job.getNegativePrompt())
                .status(job.getStatus())
                .progress(job.getProgress())
                .costCredits(job.getCostCredits())
                .outputMediaUrl(job.getOutputMediaUrl())
                .errorMessage(job.getErrorMessage())
                .projectId(job.getProject() != null ? job.getProject().getId() : null)
                .projectName(job.getProject() != null ? job.getProject().getName() : null)
                .referenceMediaUrls(refUrls)
                .createdAt(job.getCreatedAt())
                .finishedAt(job.getFinishedAt())
                .build();
    }
}
