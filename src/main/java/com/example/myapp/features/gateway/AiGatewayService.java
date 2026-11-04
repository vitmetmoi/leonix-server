package com.example.myapp.features.gateway;

import com.example.myapp.features.gateway.dto.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AiGatewayService {
    List<AiModelDto> getCatalog(String serviceType);
    AiModelDto getModel(String modelId);
    List<AiModelPricingDto> getPricing(String modelId);
    BigDecimal calculatePrice(String modelId, String capabilityCode, String resolution, String duration, Integer charCount);
    UpstreamExecutionResult dispatchWithFallback(String modelId, String capabilityCode, Map<String, Object> params);
}
