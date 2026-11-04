package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUpstreamEndpointRepository extends JpaRepository<AiUpstreamEndpoint, Long> {
    List<AiUpstreamEndpoint> findByDispatchGroupIdAndIsActiveTrueOrderByPriorityAsc(Long dispatchGroupId);
    List<AiUpstreamEndpoint> findByDispatchGroupIdOrderByPriorityAsc(Long dispatchGroupId);
    long countByIsActiveTrue();
    long countByHealthStatus(String healthStatus);
}
