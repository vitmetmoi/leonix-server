package com.example.myapp.features.gateway;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiModelPricingRepository extends JpaRepository<AiModelPricing, Long> {
    List<AiModelPricing> findByModelId(String modelId);
    Optional<AiModelPricing> findFirstByModelIdAndCapabilityCodeAndResolutionAndDuration(
            String modelId, String capabilityCode, String resolution, String duration);
    Optional<AiModelPricing> findFirstByModelIdAndCapabilityCode(String modelId, String capabilityCode);
}
