package com.example.myapp.features.workspace;

import com.example.myapp.features.auth.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "prompt_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String category; // CINEMATIC, TIKTOK_REELS, SHOPEE_ADS, FASHION, ANIME

    @Column(name = "service_type", nullable = false, length = 20)
    private String serviceType; // VIDEO, IMAGE

    @Column(name = "prompt_vi", nullable = false, columnDefinition = "TEXT")
    private String promptVi;

    @Column(name = "prompt_en", columnDefinition = "TEXT")
    private String promptEn;

    @Column(name = "preview_media_url", length = 500)
    private String previewMediaUrl;

    @Column(length = 255)
    private String tags;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "likes_count", nullable = false)
    private Integer likesCount = 0;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
