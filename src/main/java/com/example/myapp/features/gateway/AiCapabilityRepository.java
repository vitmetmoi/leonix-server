package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiCapabilityRepository extends JpaRepository<AiCapability, Long> {
    List<AiCapability> findByModelIdAndIsActiveTrue(String modelId);
    Optional<AiCapability> findByModelIdAndCapabilityCodeAndIsActiveTrue(String modelId, String capabilityCode);
}
