package com.example.myapp.features.gateway;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_dispatch_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "upstreamEndpoints")
@EqualsAndHashCode(of = "id")
public class AiDispatchGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_code", nullable = false, unique = true, length = 100)
    private String groupCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String strategy = "PRIORITY_FAILOVER"; // PRIORITY_FAILOVER, ROUND_ROBIN, WEIGHTED

    @Builder.Default
    @Column(name = "failover_status_codes", nullable = false, length = 100)
    private String failoverStatusCodes = "429,500,502,503,504";

    @Builder.Default
    @Column(name = "max_retries_per_upstream", nullable = false)
    private Integer maxRetriesPerUpstream = 1;

    @Builder.Default
    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 30000;

    @Builder.Default
    @OneToMany(mappedBy = "dispatchGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priority ASC")
    private List<AiUpstreamEndpoint> upstreamEndpoints = new ArrayList<>();

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
