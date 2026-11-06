package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUpstreamEndpointRepository extends JpaRepository<AiUpstreamEndpoint, Long> {

    @Query("SELECT e FROM AiUpstreamEndpoint e WHERE e.dispatchGroup.id = :dispatchGroupId AND e.isActive = true ORDER BY e.priority ASC")
    List<AiUpstreamEndpoint> findByDispatchGroupIdAndIsActiveTrueOrderByPriorityAsc(@Param("dispatchGroupId") Long dispatchGroupId);

    @Query("SELECT e FROM AiUpstreamEndpoint e WHERE e.dispatchGroup.id = :dispatchGroupId ORDER BY e.priority ASC")
    List<AiUpstreamEndpoint> findByDispatchGroupIdOrderByPriorityAsc(@Param("dispatchGroupId") Long dispatchGroupId);

    long countByIsActiveTrue();

    long countByHealthStatus(String healthStatus);
}
