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
    List<AdminModelResponse> listModels();
    AdminModelResponse saveModel(AdminModelRequest request);
    void deleteModel(String id);
    void attachModelToCluster(String modelId, Long dispatchGroupId);
    void toggleModelStatus(String modelId, Boolean isActive, Boolean isMaintenance);
    List<AiDispatchGroup> listDispatchGroups();
    AiDispatchGroup saveDispatchGroup(AdminDispatchGroupRequest request);
    void deleteDispatchGroup(Long id);
    AiUpstreamEndpoint saveUpstreamEndpoint(AdminUpstreamEndpointRequest request);
    void toggleUpstreamStatus(Long endpointId, Boolean isActive);
    void deleteUpstreamEndpoint(Long id);
    List<AdminPaymentResponse> listPayments(int page, int size);
    void manualApprovePayment(String paymentId);
    List<AiModelPricing> listPricing();
    AiModelPricing savePricing(AdminPricingRequest request);
    AiModelPricing updatePricing(Long id, BigDecimal sellPrice, BigDecimal costPrice);
    void deletePricing(Long id);
}
