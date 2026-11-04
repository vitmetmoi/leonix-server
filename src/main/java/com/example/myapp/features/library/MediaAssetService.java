package com.example.myapp.features.library;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.library.dto.MediaAssetResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MediaAssetService {
    List<MediaAssetResponse> listAssets(User user, String mediaType, Long projectId, Boolean isFavorite, Pageable pageable);
    MediaAssetResponse toggleFavorite(User user, Long assetId);
    void deleteAsset(User user, Long assetId);
    void batchDeleteAssets(User user, List<Long> assetIds);
}
