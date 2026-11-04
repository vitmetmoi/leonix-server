package com.example.myapp.features.gateway;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_upstream_endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"apiKeyEncrypted", "dispatchGroup"})
@EqualsAndHashCode(of = "id")
public class AiUpstreamEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_group_id", nullable = false)
    private AiDispatchGroup dispatchGroup;

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 1;

    @Builder.Default
    @Column(nullable = false)
    private Integer weight = 100;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Builder.Default
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod = "POST";

    @Column(name = "headers_template", nullable = false, columnDefinition = "TEXT")
    private String headersTemplate;

    @Column(name = "api_key_encrypted", length = 500)
    private String apiKeyEncrypted;

    @Column(name = "request_body_template", nullable = false, columnDefinition = "TEXT")
    private String requestBodyTemplate;

    @Column(name = "response_mapping", nullable = false, columnDefinition = "TEXT")
    private String responseMapping;

    @Column(name = "polling_url_template", length = 500)
    private String pollingUrlTemplate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "health_status", nullable = false, length = 20)
    private String healthStatus = "HEALTHY"; // HEALTHY, DEGRADED, SUSPENDED

    @Builder.Default
    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

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
