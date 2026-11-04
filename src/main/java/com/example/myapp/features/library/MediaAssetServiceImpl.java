package com.example.myapp.features.library;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.library.dto.MediaAssetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAssetServiceImpl implements MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> listAssets(User user, String mediaType, Long projectId, Boolean isFavorite, Pageable pageable) {
        String typeFilter = (mediaType != null && !mediaType.isBlank()) ? mediaType.toUpperCase() : null;
        return mediaAssetRepository.findAssets(user.getId(), typeFilter, projectId, isFavorite, pageable)
                .stream()
                .map(this::mapToAssetResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MediaAssetResponse toggleFavorite(User user, Long assetId) {
        MediaAsset asset = mediaAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MediaAsset", "id", assetId));

        asset.setIsFavorite(!Boolean.TRUE.equals(asset.getIsFavorite()));
        MediaAsset saved = mediaAssetRepository.save(asset);
        log.info("Toggled favorite for asset ID {}: {}", assetId, saved.getIsFavorite());
        return mapToAssetResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAsset(User user, Long assetId) {
        MediaAsset asset = mediaAssetRepository.findByIdAndUserId(assetId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("MediaAsset", "id", assetId));
        mediaAssetRepository.delete(asset);
        log.info("Deleted asset ID {} by user {}", assetId, user.getEmail());
    }

    @Override
    @Transactional
    public void batchDeleteAssets(User user, List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return;
        List<MediaAsset> assets = mediaAssetRepository.findByIdInAndUserId(assetIds, user.getId());
        mediaAssetRepository.deleteAll(assets);
        log.info("Batch deleted {} assets by user {}", assets.size(), user.getEmail());
    }

    private MediaAssetResponse mapToAssetResponse(MediaAsset asset) {
        return MediaAssetResponse.builder()
                .id(asset.getId())
                .mediaType(asset.getMediaType())
                .url(asset.getUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .fileName(asset.getFileName())
                .fileSize(asset.getFileSize())
                .durationSeconds(asset.getDurationSeconds())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .isFavorite(asset.getIsFavorite())
                .metadata(asset.getMetadata())
                .projectId(asset.getProject() != null ? asset.getProject().getId() : null)
                .projectName(asset.getProject() != null ? asset.getProject().getName() : null)
                .jobUuid(asset.getJob() != null ? asset.getJob().getJobUuid() : null)
                .createdAt(asset.getCreatedAt())
                .build();
    }
}
