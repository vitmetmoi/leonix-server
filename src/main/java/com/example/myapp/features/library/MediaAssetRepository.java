package com.example.myapp.features.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT a FROM MediaAsset a WHERE a.user.id = :userId " +
           "AND (:mediaType IS NULL OR a.mediaType = :mediaType) " +
           "AND (:projectId IS NULL OR (a.project IS NOT NULL AND a.project.id = :projectId)) " +
           "AND (:isFavorite IS NULL OR a.isFavorite = :isFavorite) " +
           "ORDER BY a.createdAt DESC")
    Page<MediaAsset> findAssets(
            @Param("userId") Long userId,
            @Param("mediaType") String mediaType,
            @Param("projectId") Long projectId,
            @Param("isFavorite") Boolean isFavorite,
            Pageable pageable
    );

    List<MediaAsset> findByIdInAndUserId(List<Long> ids, Long userId);
}
