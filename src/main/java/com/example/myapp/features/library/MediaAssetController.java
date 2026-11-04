package com.example.myapp.features.library;

import com.example.myapp.features.auth.AuthService;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.library.dto.BatchDeleteAssetsRequest;
import com.example.myapp.features.library.dto.MediaAssetResponse;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
@Slf4j
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> listAssets(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        User user = authService.getCurrentUser();
        List<MediaAssetResponse> assets = mediaAssetService.listAssets(user, type, projectId, favorite, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> toggleFavorite(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        MediaAssetResponse response = mediaAssetService.toggleFavorite(user, id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái yêu thích thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        mediaAssetService.deleteAsset(user, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài sản thành công", null));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Void>> batchDeleteAssets(@Valid @RequestBody BatchDeleteAssetsRequest request) {
        User user = authService.getCurrentUser();
        mediaAssetService.batchDeleteAssets(user, request.getAssetIds());
        return ResponseEntity.ok(ApiResponse.success("Xóa hàng loạt tài sản thành công", null));
    }
}
