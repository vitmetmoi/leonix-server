package com.example.myapp.features.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_llm_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "apiKeyEncrypted")
@EqualsAndHashCode(of = "id")
public class UserLlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Builder.Default
    @Column(name = "base_url", nullable = false)
    private String baseUrl = "https://api.openai.com/v1";

    @Column(name = "api_key_encrypted", nullable = false, length = 500)
    private String apiKeyEncrypted;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

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
