package com.example.myapp.features.admin;

import com.example.myapp.features.admin.dto.*;
import com.example.myapp.features.gateway.AiDispatchGroup;
import com.example.myapp.features.gateway.AiModel;
import com.example.myapp.features.gateway.AiModelPricing;
import com.example.myapp.features.gateway.AiUpstreamEndpoint;
import com.example.myapp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOverview()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers(query, role, page, size)));
    }

    @PostMapping("/users/{id}/adjust-balance")
    public ResponseEntity<ApiResponse<Void>> adjustBalance(
            @PathVariable Long id,
            @Valid @RequestBody AdminAdjustBalanceRequest request
    ) {
        adminService.adjustUserBalance(id, request.getAmount(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Điều chỉnh số dư thành công", null));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        adminService.updateUserRole(id, body.get("role"));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền thành công", null));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<AdminJobResponse>>> listJobs(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listJobs(serviceType, status, page, size)));
    }

    @PostMapping("/jobs/{jobUuid}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelJob(@PathVariable String jobUuid) {
        adminService.cancelAndRefundJob(jobUuid);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy và hoàn tiền tác vụ", null));
    }

    @PostMapping("/jobs/{jobUuid}/retry")
    public ResponseEntity<ApiResponse<Void>> retryJob(@PathVariable String jobUuid) {
        adminService.retryJob(jobUuid);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu chạy lại tác vụ", null));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<AdminModelResponse>>> listModels() {
        return ResponseEntity.ok(ApiResponse.success(adminService.listModels()));
    }

    @PostMapping("/models")
    public ResponseEntity<ApiResponse<AdminModelResponse>> saveModel(@Valid @RequestBody AdminModelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.saveModel(request)));
    }

    @DeleteMapping("/models")
    public ResponseEntity<ApiResponse<Void>> deleteModel(@RequestParam String id) {
        adminService.deleteModel(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa AI model thành công", null));
    }

    @PostMapping("/models/attach-cluster")
    public ResponseEntity<ApiResponse<Void>> attachModelToCluster(
            @RequestBody Map<String, Object> body
    ) {
        String modelId = (String) body.get("modelId");
        Long dispatchGroupId = body.get("dispatchGroupId") != null ? Long.valueOf(body.get("dispatchGroupId").toString()) : null;
        adminService.attachModelToCluster(modelId, dispatchGroupId);
        return ResponseEntity.ok(ApiResponse.success("Gắn cụm điều phối cho model thành công", null));
    }

    @PutMapping("/models/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleModelStatus(
            @RequestBody Map<String, Object> body
    ) {
        String modelId = (String) body.get("modelId");
        Boolean isActive = (Boolean) body.get("isActive");
        Boolean isMaintenance = (Boolean) body.get("isMaintenance");
        adminService.toggleModelStatus(modelId, isActive, isMaintenance);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái model thành công", null));
    }

    @GetMapping("/dispatch-groups")
    public ResponseEntity<ApiResponse<List<AiDispatchGroup>>> listDispatchGroups() {
        return ResponseEntity.ok(ApiResponse.success(adminService.listDispatchGroups()));
    }

    @PostMapping("/dispatch-groups")
    public ResponseEntity<ApiResponse<AiDispatchGroup>> saveDispatchGroup(
            @Valid @RequestBody AdminDispatchGroupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.saveDispatchGroup(request)));
    }

    @DeleteMapping("/dispatch-groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDispatchGroup(@PathVariable Long id) {
        adminService.deleteDispatchGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cụm điều phối thành công", null));
    }

    @PostMapping("/upstream-endpoints")
    public ResponseEntity<ApiResponse<AiUpstreamEndpoint>> saveUpstreamEndpoint(
            @Valid @RequestBody AdminUpstreamEndpointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.saveUpstreamEndpoint(request)));
    }

    @PutMapping("/upstream-endpoints/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUpstreamStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        adminService.toggleUpstreamStatus(id, body.get("isActive"));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái upstream node thành công", null));
    }

    @DeleteMapping("/upstream-endpoints/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUpstreamEndpoint(@PathVariable Long id) {
        adminService.deleteUpstreamEndpoint(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa upstream endpoint node thành công", null));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<AdminPaymentResponse>>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listPayments(page, size)));
    }

    @PostMapping("/payments/{paymentId}/approve")
    public ResponseEntity<ApiResponse<Void>> manualApprovePayment(@PathVariable String paymentId) {
        adminService.manualApprovePayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt nạp tiền thủ công", null));
    }

    @GetMapping("/pricing")
    public ResponseEntity<ApiResponse<List<AiModelPricing>>> listPricing() {
        return ResponseEntity.ok(ApiResponse.success(adminService.listPricing()));
    }

    @PostMapping("/pricing")
    public ResponseEntity<ApiResponse<AiModelPricing>> savePricing(@Valid @RequestBody AdminPricingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.savePricing(request)));
    }

    @PutMapping("/pricing/{id}")
    public ResponseEntity<ApiResponse<AiModelPricing>> updatePricing(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> body
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updatePricing(id, body.get("sellPrice"), body.get("costPrice"))));
    }

    @DeleteMapping("/pricing/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePricing(@PathVariable Long id) {
        adminService.deletePricing(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa cấu hình đơn giá thành công", null));
    }
}
