package com.example.myapp.features.generator;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    Optional<AiJob> findByJobUuidAndUserId(String jobUuid, Long userId);
    Optional<AiJob> findByJobUuid(String jobUuid);

    @Query("SELECT j FROM AiJob j WHERE j.user.id = :userId " +
           "AND (:serviceType IS NULL OR j.serviceType = :serviceType) " +
           "AND (:status IS NULL OR j.status = :status) " +
           "ORDER BY j.createdAt DESC")
    List<AiJob> findJobs(
            @Param("userId") Long userId,
            @Param("serviceType") String serviceType,
            @Param("status") String status,
            Pageable pageable
    );

    List<AiJob> findByJobUuidInAndUserId(List<String> jobUuids, Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(j.costCredits), 0) FROM AiJob j WHERE j.user.id = :userId AND j.status = 'COMPLETED'")
    Long sumCompletedCreditsByUserId(@Param("userId") Long userId);

    @Query("SELECT j FROM AiJob j WHERE " +
           "(:serviceType IS NULL OR j.serviceType = :serviceType) AND " +
           "(:status IS NULL OR j.status = :status) " +
           "ORDER BY j.createdAt DESC")
    List<AiJob> findAdminJobs(
            @Param("serviceType") String serviceType,
            @Param("status") String status,
            Pageable pageable
    );

    List<AiJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);

    long countByServiceType(String serviceType);
}
