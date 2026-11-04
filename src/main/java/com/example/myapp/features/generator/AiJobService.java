package com.example.myapp.features.generator;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.generator.dto.*;

import java.util.List;

public interface AiJobService {
    AiJobResponse createVideoJob(User user, CreateVideoJobRequest request);
    AiJobResponse createImageJob(User user, CreateImageJobRequest request);
    AiJobResponse createVoiceJob(User user, CreateVoiceJobRequest request);
    List<AiJobResponse> listJobs(User user, String serviceType, String status, int limit);
    AiJobResponse getJob(User user, String jobUuid);
    void deleteJob(User user, String jobUuid);
    void batchDeleteJobs(User user, List<String> jobUuids);
}
