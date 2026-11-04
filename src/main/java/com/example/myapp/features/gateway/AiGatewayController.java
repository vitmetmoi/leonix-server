package com.example.myapp.features.gateway;

import com.example.myapp.features.gateway.dto.*;
import com.example.myapp.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AiGatewayController {

    private final AiGatewayService aiGatewayService;

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<AiModelDto>>> getModels(@RequestParam(required = false) String serviceType) {
        List<AiModelDto> models = aiGatewayService.getCatalog(serviceType);
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/models/{id}")
    public ResponseEntity<ApiResponse<AiModelDto>> getModel(@PathVariable String id) {
        AiModelDto model = aiGatewayService.getModel(id);
        return ResponseEntity.ok(ApiResponse.success(model));
    }

    @GetMapping("/pricing")
    public ResponseEntity<ApiResponse<List<AiModelPricingDto>>> getPricing(@RequestParam(required = false) String modelId) {
        List<AiModelPricingDto> pricing = aiGatewayService.getPricing(modelId);
        return ResponseEntity.ok(ApiResponse.success(pricing));
    }

    @PostMapping("/gateway/test-fallback")
    public ResponseEntity<ApiResponse<UpstreamExecutionResult>> testFallback(
            @RequestParam String modelId,
            @RequestParam String capabilityCode,
            @RequestBody(required = false) Map<String, Object> params
    ) {
        log.info("Testing fallback dispatch chain for model: {}, capability: {}", modelId, capabilityCode);
        UpstreamExecutionResult result = aiGatewayService.dispatchWithFallback(modelId, capabilityCode, params != null ? params : Map.of());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
