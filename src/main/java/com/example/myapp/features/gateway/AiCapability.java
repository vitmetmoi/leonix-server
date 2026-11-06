package com.example.myapp.features.gateway;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ai_capabilities",
    uniqueConstraints = @UniqueConstraint(name = "uq_model_capability", columnNames = {"model_id", "capability_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"model", "dispatchGroup"})
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private AiModel model;

    @Column(name = "capability_code", nullable = false, length = 30)
    private String capabilityCode; // t2v, i2v, r2v, startend, t2i, i2i, tts

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "supported_resolutions", length = 255)
    private String supportedResolutions; // e.g. "480p,720p,1080p"

    @Column(name = "supported_durations", length = 255)
    private String supportedDurations; // e.g. "5s,10s,15s"

    @Column(name = "supported_aspect_ratios", length = 255)
    private String supportedAspectRatios; // e.g. "16:9,9:16,1:1"

    @JsonIgnoreProperties({"upstreamEndpoints", "hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_group_id", nullable = false)
    private AiDispatchGroup dispatchGroup;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
