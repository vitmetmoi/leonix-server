package com.example.myapp.features.generator;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_reference_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "job")
@EqualsAndHashCode(of = "id")
public class JobReferenceMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private AiJob job;

    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType; // START_FRAME, END_FRAME, STYLE_REFERENCE, VOICE_SAMPLE

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
