package com.example.myapp.features.gateway;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"capabilitiesList", "pricingList"})
@EqualsAndHashCode(of = "id")
public class AiModel {

    @Id
    @Column(nullable = false, length = 100)
    private String id; // e.g. xai/grok-imagine-video, google/nano-banana-2

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType; // VIDEO, IMAGE, VOICE

    @Column(columnDefinition = "TEXT")
    private String capabilities; // JSON metadata for client ui capabilities

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_maintenance", nullable = false)
    private Boolean isMaintenance = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiCapability> capabilitiesList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiModelPricing> pricingList = new ArrayList<>();

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
