package com.example.myapp.features.admin;

import com.example.myapp.features.admin.dto.*;
import com.example.myapp.features.gateway.AiDispatchGroup;
import com.example.myapp.features.gateway.AiModel;
import com.example.myapp.features.gateway.AiModelPricing;
import com.example.myapp.features.gateway.AiUpstreamEndpoint;

import java.math.BigDecimal;
import java.util.List;

public interface AdminService {
    AdminOverviewResponse getOverview();
    List<AdminUserResponse> listUsers(String query, String role, int page, int size);
    void adjustUserBalance(Long userId, BigDecimal amount, String reason);
    void updateUserRole(Long userId, String role);
    List<AdminJobResponse> listJobs(String serviceType, String status, int page, int size);
    void cancelAndRefundJob(String jobUuid);
    void retryJob(String jobUuid);
    List<AiModel> listModels();
    void toggleModelStatus(String modelId, Boolean isActive, Boolean isMaintenance);
    List<AiDispatchGroup> listDispatchGroups();
    AiUpstreamEndpoint saveUpstreamEndpoint(AdminUpstreamEndpointRequest request);
    void toggleUpstreamStatus(Long endpointId, Boolean isActive);
    List<AdminPaymentResponse> listPayments(int page, int size);
    void manualApprovePayment(String paymentId);
    List<AiModelPricing> listPricing();
    AiModelPricing updatePricing(Long id, BigDecimal sellPrice, BigDecimal costPrice);
}
