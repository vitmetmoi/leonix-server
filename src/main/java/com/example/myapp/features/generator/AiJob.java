package com.example.myapp.features.generator;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.gateway.AiModel;
import com.example.myapp.features.workspace.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "project", "model", "referenceMediaList"})
@EqualsAndHashCode(of = "id")
public class AiJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_uuid", nullable = false, unique = true, length = 64)
    private String jobUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private AiModel model;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType; // VIDEO, IMAGE, VOICE

    @Column(name = "input_mode", nullable = false, length = 20)
    private String inputMode; // t2v, i2v, r2v, startend, t2i, i2i, tts

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    @Column(columnDefinition = "TEXT")
    private String params; // JSON containing resolution, duration, aspect_ratio, lens, etc.

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "QUEUED"; // QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED

    @Builder.Default
    @Column(nullable = false)
    private Integer progress = 0;

    @Builder.Default
    @Column(name = "cost_credits", nullable = false, precision = 12, scale = 2)
    private BigDecimal costCredits = BigDecimal.ZERO;

    @Column(name = "output_media_url", length = 500)
    private String outputMediaUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "worker_task_id", length = 100)
    private String workerTaskId;

    @Builder.Default
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobReferenceMedia> referenceMediaList = new ArrayList<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
