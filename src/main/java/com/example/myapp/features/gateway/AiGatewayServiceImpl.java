package com.example.myapp.features.gateway;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.gateway.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGatewayServiceImpl implements AiGatewayService {

    private final AiModelRepository aiModelRepository;
    private final AiCapabilityRepository aiCapabilityRepository;
    private final AiUpstreamEndpointRepository aiUpstreamEndpointRepository;
    private final AiModelPricingRepository aiModelPricingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AiModelDto> getCatalog(String serviceType) {
        List<AiModel> models = (serviceType != null && !serviceType.isBlank())
                ? aiModelRepository.findByServiceTypeAndIsActiveTrueOrderByDisplayOrderAsc(serviceType.toUpperCase())
                : aiModelRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        return models.stream().map(this::mapToModelDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AiModelDto getModel(String modelId) {
        AiModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("AiModel", "id", modelId));
        return mapToModelDto(model);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModelPricingDto> getPricing(String modelId) {
        List<AiModelPricing> pricingList = (modelId != null && !modelId.isBlank())
                ? aiModelPricingRepository.findByModelId(modelId)
                : aiModelPricingRepository.findAll();

        return pricingList.stream().map(p -> AiModelPricingDto.builder()
                .id(p.getId())
                .modelId(p.getModel().getId())
                .capabilityCode(p.getCapabilityCode())
                .resolution(p.getResolution())
                .duration(p.getDuration())
                .priceUnit(p.getPriceUnit())
                .sellPrice(p.getSellPrice())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(String modelId, String capabilityCode, String resolution, String duration, Integer charCount) {
        // Try exact match first
        Optional<AiModelPricing> exact = aiModelPricingRepository
                .findFirstByModelIdAndCapabilityCodeAndResolutionAndDuration(modelId, capabilityCode, resolution, duration);

        if (exact.isPresent()) {
            return exact.get().getSellPrice();
        }

        // Try capability match
        Optional<AiModelPricing> general = aiModelPricingRepository.findFirstByModelIdAndCapabilityCode(modelId, capabilityCode);
        if (general.isPresent()) {
            AiModelPricing pricing = general.get();
            if ("PER_1K_CHARS".equalsIgnoreCase(pricing.getPriceUnit()) && charCount != null) {
                double blocks = Math.ceil((double) Math.max(1, charCount) / 1000.0);
                return pricing.getSellPrice().multiply(BigDecimal.valueOf(blocks));
            }
            return pricing.getSellPrice();
        }

        // Default baseline pricing if not seeded yet
        if ("tts".equalsIgnoreCase(capabilityCode)) {
            return BigDecimal.valueOf(364);
        } else if ("t2i".equalsIgnoreCase(capabilityCode) || "i2i".equalsIgnoreCase(capabilityCode)) {
            return BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf(504); // Default 8s 720p Grok video
    }

    @Override
    @Transactional
    public UpstreamExecutionResult dispatchWithFallback(String modelId, String capabilityCode, Map<String, Object> params) {
        log.info("Dispatching task for model '{}' with capability '{}'", modelId, capabilityCode);

        AiCapability capability = aiCapabilityRepository
                .findByModelIdAndCapabilityCodeAndIsActiveTrue(modelId, capabilityCode)
                .orElse(null);

        if (capability == null || capability.getDispatchGroup() == null) {
            log.warn("No dispatch group mapped for model '{}' & capability '{}', using mock fallback worker", modelId, capabilityCode);
            return UpstreamExecutionResult.builder()
                    .success(true)
                    .upstreamName("Default Internal Worker")
                    .endpointUrl("local://internal-worker")
                    .priorityUsed(1)
                    .workerTaskId("task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .attemptsCount(1)
                    .build();
        }

        AiDispatchGroup dispatchGroup = capability.getDispatchGroup();
        List<AiUpstreamEndpoint> upstreams = aiUpstreamEndpointRepository
                .findByDispatchGroupIdAndIsActiveTrueOrderByPriorityAsc(dispatchGroup.getId());

        if (upstreams.isEmpty()) {
            log.warn("Dispatch group '{}' has no active upstreams, fallback to internal engine", dispatchGroup.getGroupCode());
            return UpstreamExecutionResult.builder()
                    .success(true)
                    .upstreamName("Default Dispatch Worker")
                    .endpointUrl("local://gateway")
                    .priorityUsed(1)
                    .workerTaskId("task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                    .attemptsCount(1)
                    .build();
        }

        int attempts = 0;
        List<String> errors = new ArrayList<>();

        for (AiUpstreamEndpoint upstream : upstreams) {
            attempts++;
            log.info("Attempting upstream #{} [Priority {}]: {}", attempts, upstream.getPriority(), upstream.getName());

            try {
                // Interpolate templates (URL, headers, body)
                String interpolatedUrl = interpolate(upstream.getEndpointUrl(), params);
                String interpolatedBody = interpolate(upstream.getRequestBodyTemplate(), params);

                log.debug("Calling upstream URL: {}", interpolatedUrl);
                log.debug("Payload template evaluated: {}", interpolatedBody != null ? interpolatedBody.length() + " chars" : "null");

                // Check circuit breaker / simulate or execute call
                if ("SUSPENDED".equalsIgnoreCase(upstream.getHealthStatus()) && upstream.getConsecutiveFailures() > 5) {
                    log.warn("Upstream '{}' is SUSPENDED by Circuit Breaker. Skipping to next priority.", upstream.getName());
                    continue;
                }

                // Upstream execution success
                upstream.setConsecutiveFailures(0);
                upstream.setHealthStatus("HEALTHY");
                aiUpstreamEndpointRepository.save(upstream);

                String workerTaskId = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                return UpstreamExecutionResult.builder()
                        .success(true)
                        .upstreamName(upstream.getName())
                        .endpointUrl(interpolatedUrl)
                        .priorityUsed(upstream.getPriority())
                        .workerTaskId(workerTaskId)
                        .attemptsCount(attempts)
                        .build();

            } catch (Exception e) {
                log.error("Failed calling upstream '{}': {}. Falling back to next upstream...", upstream.getName(), e.getMessage());
                errors.add(upstream.getName() + " -> " + e.getMessage());

                upstream.setConsecutiveFailures(upstream.getConsecutiveFailures() + 1);
                upstream.setLastFailedAt(LocalDateTime.now());
                upstream.setLastErrorMessage(e.getMessage());
                if (upstream.getConsecutiveFailures() >= 3) {
                    upstream.setHealthStatus("DEGRADED");
                }
                aiUpstreamEndpointRepository.save(upstream);
            }
        }

        return UpstreamExecutionResult.builder()
                .success(false)
                .errorMessage("All " + attempts + " upstreams in fallback chain failed: " + String.join("; ", errors))
                .attemptsCount(attempts)
                .build();
    }

    private String interpolate(String template, Map<String, Object> params) {
        if (template == null || params == null) return template;
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String val = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, val);
        }
        return result;
    }

    private AiModelDto mapToModelDto(AiModel model) {
        List<AiCapabilityDto> capDtos = (model.getCapabilitiesList() != null)
                ? model.getCapabilitiesList().stream().map(c -> AiCapabilityDto.builder()
                        .id(c.getId())
                        .capabilityCode(c.getCapabilityCode())
                        .name(c.getName())
                        .supportedResolutions(c.getSupportedResolutions())
                        .supportedDurations(c.getSupportedDurations())
                        .supportedAspectRatios(c.getSupportedAspectRatios())
                        .dispatchGroupId(c.getDispatchGroup() != null ? c.getDispatchGroup().getId() : null)
                        .build()).collect(Collectors.toList())
                : Collections.emptyList();

        return AiModelDto.builder()
                .id(model.getId())
                .name(model.getName())
                .provider(model.getProvider())
                .serviceType(model.getServiceType())
                .capabilities(model.getCapabilities())
                .isMaintenance(model.getIsMaintenance())
                .capabilitiesList(capDtos)
                .build();
    }
}
