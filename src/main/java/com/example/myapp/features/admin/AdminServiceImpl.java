package com.example.myapp.features.admin;

import com.example.myapp.exception.ResourceNotFoundException;
import com.example.myapp.features.admin.dto.*;
import com.example.myapp.features.auth.User;
import com.example.myapp.features.auth.UserRepository;
import com.example.myapp.features.gateway.*;
import com.example.myapp.features.generator.AiJob;
import com.example.myapp.features.generator.AiJobRepository;
import com.example.myapp.features.wallet.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final AiJobRepository aiJobRepository;
    private final AiModelRepository aiModelRepository;
    private final AiCapabilityRepository capabilityRepository;
    private final AiDispatchGroupRepository dispatchGroupRepository;
    private final AiUpstreamEndpointRepository upstreamEndpointRepository;
    private final AiModelPricingRepository pricingRepository;
    private final WalletService walletService;

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        log.info("Fetching admin system overview metrics");

        long totalUsers = userRepository.count();
        long totalJobs = aiJobRepository.count();
        BigDecimal totalRevenue = paymentRepository.sumTotalSuccessfulPayments();

        long activeUpstreams = upstreamEndpointRepository.countByIsActiveTrue();
        long healthyUpstreams = upstreamEndpointRepository.countByHealthStatus("HEALTHY");

        Map<String, Long> jobsByService = new LinkedHashMap<>();
        jobsByService.put("VIDEO", aiJobRepository.countByServiceType("VIDEO"));
        jobsByService.put("IMAGE", aiJobRepository.countByServiceType("IMAGE"));
        jobsByService.put("VOICE", aiJobRepository.countByServiceType("VOICE"));

        Map<String, Long> jobsByStatus = new LinkedHashMap<>();
        jobsByStatus.put("QUEUED", aiJobRepository.countByStatus("QUEUED"));
        jobsByStatus.put("PROCESSING", aiJobRepository.countByStatus("PROCESSING"));
        jobsByStatus.put("COMPLETED", aiJobRepository.countByStatus("COMPLETED"));
        jobsByStatus.put("FAILED", aiJobRepository.countByStatus("FAILED"));

        List<AdminJobResponse> recentJobs = aiJobRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream().map(this::mapToAdminJob).collect(Collectors.toList());

        List<AdminPaymentResponse> recentPayments = paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream().map(this::mapToAdminPayment).collect(Collectors.toList());

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalJobs(totalJobs)
                .totalRevenueVnd(totalRevenue)
                .activeUpstreamsCount(activeUpstreams)
                .healthyUpstreamsCount(healthyUpstreams)
                .jobsByService(jobsByService)
                .jobsByStatus(jobsByStatus)
                .recentJobs(recentJobs)
                .recentPayments(recentPayments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String query, String role, int page, int size) {
        log.info("Listing users for admin: query={}, role={}, page={}, size={}", query, role, page, size);
        return userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .filter(u -> query == null || query.isBlank() || u.getEmail().toLowerCase().contains(query.toLowerCase()) || u.getFullName().toLowerCase().contains(query.toLowerCase()))
                .filter(u -> role == null || role.isBlank() || u.getRole().equalsIgnoreCase(role))
                .map(u -> {
                    Wallet w = walletRepository.findByUserId(u.getId()).orElse(null);
                    long userJobs = aiJobRepository.countByUserId(u.getId());
                    return AdminUserResponse.builder()
                            .id(u.getId())
                            .email(u.getEmail())
                            .fullName(u.getFullName())
                            .role(u.getRole())
                            .status(u.getStatus())
                            .balance(w != null ? w.getBalance() : BigDecimal.ZERO)
                            .reservedBalance(w != null ? w.getReserved() : BigDecimal.ZERO)
                            .totalJobs(userJobs)
                            .createdAt(u.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void adjustUserBalance(Long userId, BigDecimal amount, String reason) {
        log.info("Admin adjusting balance for userId: {}, amount: {}, reason: {}", userId, amount, reason);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Wallet wallet = walletService.getOrCreateWallet(user);

        BigDecimal newBalance = wallet.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số dư ví không thể âm sau khi điều chỉnh");
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(amount.compareTo(BigDecimal.ZERO) >= 0 ? "DEPOSIT" : "WITHDRAW")
                .amount(amount.abs())
                .balanceAfter(newBalance)
                .referenceType("ADMIN_ADJUST")
                .referenceId("adj_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .description("Admin điều chỉnh: " + (reason != null ? reason : "Thay đổi số dư thủ công"))
                .build());
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, String role) {
        log.info("Admin updating role for userId: {} to {}", userId, role);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setRole(role.toUpperCase());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminJobResponse> listJobs(String serviceType, String status, int page, int size) {
        return aiJobRepository.findAdminJobs(serviceType, status, PageRequest.of(page, size))
                .stream().map(this::mapToAdminJob).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelAndRefundJob(String jobUuid) {
        log.info("Admin cancelling and refunding jobUuid: {}", jobUuid);
        AiJob job = aiJobRepository.findByJobUuid(jobUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", "jobUuid", jobUuid));

        if ("COMPLETED".equals(job.getStatus())) {
            throw new IllegalStateException("Không thể hủy job đã hoàn thành");
        }

        job.setStatus("FAILED");
        job.setErrorMessage("Đã hủy bởi Quản trị viên");
        aiJobRepository.save(job);

        walletService.refundCredits(job.getUser(), job.getCostCredits(), job.getJobUuid());
    }

    @Override
    @Transactional
    public void retryJob(String jobUuid) {
        log.info("Admin retrying jobUuid: {}", jobUuid);
        AiJob job = aiJobRepository.findByJobUuid(jobUuid)
                .orElseThrow(() -> new ResourceNotFoundException("AiJob", "jobUuid", jobUuid));

        job.setStatus("QUEUED");
        job.setProgress(0);
        job.setErrorMessage(null);
        aiJobRepository.save(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminModelResponse> listModels() {
        List<AiModel> models = aiModelRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));
        List<AdminModelResponse> responses = new ArrayList<>();

        for (AiModel m : models) {
            List<AiCapability> caps = capabilityRepository.findByModelIdAndIsActiveTrue(m.getId());
            List<AiModelPricing> pricing = pricingRepository.findByModelId(m.getId());

            Long dgId = null;
            String dgCode = null;
            String dgName = null;
            for (AiCapability cap : caps) {
                if (cap.getDispatchGroup() != null) {
                    dgId = cap.getDispatchGroup().getId();
                    dgCode = cap.getDispatchGroup().getGroupCode();
                    dgName = cap.getDispatchGroup().getName();
                    break;
                }
            }

            responses.add(AdminModelResponse.builder()
                    .id(m.getId())
                    .name(m.getName())
                    .provider(m.getProvider())
                    .serviceType(m.getServiceType())
                    .capabilities(m.getCapabilities())
                    .isActive(m.getIsActive())
                    .isMaintenance(m.getIsMaintenance())
                    .displayOrder(m.getDisplayOrder())
                    .dispatchGroupId(dgId)
                    .dispatchGroupCode(dgCode)
                    .dispatchGroupName(dgName)
                    .pricingCount(pricing.size())
                    .createdAt(m.getCreatedAt())
                    .updatedAt(m.getUpdatedAt())
                    .build());
        }
        return responses;
    }

    @Override
    @Transactional
    public AdminModelResponse saveModel(AdminModelRequest request) {
        String modelId = request.getId().trim();
        AiModel model = aiModelRepository.findById(modelId).orElseGet(AiModel::new);
        model.setId(modelId);
        model.setName(request.getName().trim());
        model.setProvider(request.getProvider().trim());
        model.setServiceType(request.getServiceType().trim().toUpperCase());
        model.setCapabilities(request.getCapabilities() != null && !request.getCapabilities().isBlank() ? request.getCapabilities().trim() : "{}");
        model.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        model.setIsMaintenance(request.getIsMaintenance() != null ? request.getIsMaintenance() : false);
        model.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);

        AiModel saved = aiModelRepository.save(model);

        if (request.getDispatchGroupId() != null) {
            attachModelToCluster(saved.getId(), request.getDispatchGroupId());
        }

        return listModels().stream()
                .filter(m -> m.getId().equals(saved.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteModel(String id) {
        if (!aiModelRepository.existsById(id)) {
            throw new ResourceNotFoundException("AiModel", "id", id);
        }
        List<AiCapability> caps = capabilityRepository.findByModelIdAndIsActiveTrue(id);
        capabilityRepository.deleteAll(caps);

        List<AiModelPricing> pricings = pricingRepository.findByModelId(id);
        pricingRepository.deleteAll(pricings);

        aiModelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void attachModelToCluster(String modelId, Long dispatchGroupId) {
        AiModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("AiModel", "id", modelId));
        AiDispatchGroup dg = dispatchGroupRepository.findById(dispatchGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("AiDispatchGroup", "id", dispatchGroupId));

        List<AiCapability> caps = capabilityRepository.findByModelIdAndIsActiveTrue(modelId);
        if (caps.isEmpty()) {
            String defaultCap = "VOICE".equalsIgnoreCase(model.getServiceType()) ? "tts"
                    : "IMAGE".equalsIgnoreCase(model.getServiceType()) ? "t2i" : "t2v";
            AiCapability newCap = AiCapability.builder()
                    .model(model)
                    .capabilityCode(defaultCap)
                    .name(model.getName() + " Standard")
                    .dispatchGroup(dg)
                    .isActive(true)
                    .build();
            capabilityRepository.save(newCap);
        } else {
            for (AiCapability cap : caps) {
                cap.setDispatchGroup(dg);
                capabilityRepository.save(cap);
            }
        }
    }

    @Override
    @Transactional
    public void toggleModelStatus(String modelId, Boolean isActive, Boolean isMaintenance) {
        AiModel model = aiModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("AiModel", "id", modelId));
        if (isActive != null) model.setIsActive(isActive);
        if (isMaintenance != null) model.setIsMaintenance(isMaintenance);
        aiModelRepository.save(model);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiDispatchGroup> listDispatchGroups() {
        return dispatchGroupRepository.findAll();
    }

    @Override
    @Transactional
    public AiDispatchGroup saveDispatchGroup(AdminDispatchGroupRequest request) {
        AiDispatchGroup group;
        if (request.getId() != null) {
            group = dispatchGroupRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("AiDispatchGroup", "id", request.getId()));
        } else {
            group = new AiDispatchGroup();
        }

        group.setGroupCode(request.getGroupCode().trim());
        group.setName(request.getName().trim());
        group.setStrategy(request.getStrategy() != null ? request.getStrategy() : "PRIORITY_FAILOVER");
        group.setFailoverStatusCodes(request.getFailoverStatusCodes() != null ? request.getFailoverStatusCodes() : "429,500,502,503,504");
        group.setMaxRetriesPerUpstream(request.getMaxRetriesPerUpstream() != null ? request.getMaxRetriesPerUpstream() : 1);
        group.setTimeoutMs(request.getTimeoutMs() != null ? request.getTimeoutMs() : 30000);

        return dispatchGroupRepository.save(group);
    }

    @Override
    @Transactional
    public void deleteDispatchGroup(Long id) {
        if (!dispatchGroupRepository.existsById(id)) {
            throw new ResourceNotFoundException("AiDispatchGroup", "id", id);
        }
        dispatchGroupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public AiUpstreamEndpoint saveUpstreamEndpoint(AdminUpstreamEndpointRequest request) {
        AiDispatchGroup dg = dispatchGroupRepository.findById(request.getDispatchGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("AiDispatchGroup", "id", request.getDispatchGroupId()));

        AiUpstreamEndpoint endpoint;
        if (request.getId() != null) {
            endpoint = upstreamEndpointRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("AiUpstreamEndpoint", "id", request.getId()));
        } else {
            endpoint = new AiUpstreamEndpoint();
            endpoint.setDispatchGroup(dg);
        }

        endpoint.setName(request.getName());
        endpoint.setPriority(request.getPriority() != null ? request.getPriority() : 1);
        endpoint.setWeight(request.getWeight() != null ? request.getWeight() : 100);
        endpoint.setEndpointUrl(request.getEndpointUrl());
        endpoint.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod() : "POST");
        endpoint.setHeadersTemplate(request.getHeadersTemplate() != null ? request.getHeadersTemplate() : "{}");
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            endpoint.setApiKeyEncrypted(request.getApiKey());
        }
        endpoint.setRequestBodyTemplate(request.getRequestBodyTemplate() != null ? request.getRequestBodyTemplate() : "{}");
        endpoint.setResponseMapping(request.getResponseMapping() != null ? request.getResponseMapping() : "{}");
        endpoint.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        endpoint.setHealthStatus("HEALTHY");

        return upstreamEndpointRepository.save(endpoint);
    }

    @Override
    @Transactional
    public void toggleUpstreamStatus(Long endpointId, Boolean isActive) {
        AiUpstreamEndpoint endpoint = upstreamEndpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResourceNotFoundException("AiUpstreamEndpoint", "id", endpointId));
        endpoint.setIsActive(isActive);
        upstreamEndpointRepository.save(endpoint);
    }

    @Override
    @Transactional
    public void deleteUpstreamEndpoint(Long id) {
        if (!upstreamEndpointRepository.existsById(id)) {
            throw new ResourceNotFoundException("AiUpstreamEndpoint", "id", id);
        }
        upstreamEndpointRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPaymentResponse> listPayments(int page, int size) {
        return paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .stream().map(this::mapToAdminPayment).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void manualApprovePayment(String paymentId) {
        log.info("Admin manually approving paymentId: {}", paymentId);
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        if ("COMPLETED".equals(payment.getStatus()) || "SUCCESS".equals(payment.getStatus())) {
            log.info("Payment already approved: {}", paymentId);
            return;
        }

        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Wallet wallet = payment.getWallet();
        BigDecimal newBalance = wallet.getBalance().add(payment.getCredits());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .transactionType("DEPOSIT")
                .amount(payment.getCredits())
                .balanceAfter(newBalance)
                .referenceType("PAYMENT")
                .referenceId(payment.getPaymentId())
                .description("Admin duyệt nạp tiền thủ công qua VietQR")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModelPricing> listPricing() {
        return pricingRepository.findAll();
    }

    @Override
    @Transactional
    public AiModelPricing savePricing(AdminPricingRequest request) {
        AiModel model = aiModelRepository.findById(request.getModelId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("AiModel", "id", request.getModelId()));

        AiModelPricing pricing;
        if (request.getId() != null) {
            pricing = pricingRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("AiModelPricing", "id", request.getId()));
        } else {
            pricing = new AiModelPricing();
        }

        pricing.setModel(model);
        pricing.setCapabilityCode(request.getCapabilityCode().trim());
        pricing.setResolution(request.getResolution() != null && !request.getResolution().isBlank() ? request.getResolution().trim() : null);
        pricing.setDuration(request.getDuration() != null && !request.getDuration().isBlank() ? request.getDuration().trim() : null);
        pricing.setPriceUnit(request.getPriceUnit() != null ? request.getPriceUnit().trim() : "PER_CALL");
        pricing.setSellPrice(request.getSellPrice());
        pricing.setCostPrice(request.getCostPrice());

        return pricingRepository.save(pricing);
    }

    @Override
    @Transactional
    public AiModelPricing updatePricing(Long id, BigDecimal sellPrice, BigDecimal costPrice) {
        AiModelPricing pricing = pricingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AiModelPricing", "id", id));
        if (sellPrice != null) pricing.setSellPrice(sellPrice);
        if (costPrice != null) pricing.setCostPrice(costPrice);
        return pricingRepository.save(pricing);
    }

    @Override
    @Transactional
    public void deletePricing(Long id) {
        if (!pricingRepository.existsById(id)) {
            throw new ResourceNotFoundException("AiModelPricing", "id", id);
        }
        pricingRepository.deleteById(id);
    }

    private AdminJobResponse mapToAdminJob(AiJob j) {
        return AdminJobResponse.builder()
                .id(j.getId())
                .jobUuid(j.getJobUuid())
                .userEmail(j.getUser() != null ? j.getUser().getEmail() : "system")
                .serviceType(j.getServiceType())
                .model(j.getModel() != null ? j.getModel().getName() : "Unknown")
                .status(j.getStatus())
                .progress(j.getProgress())
                .costCredits(j.getCostCredits())
                .prompt(j.getPrompt())
                .outputUrl(j.getOutputMediaUrl())
                .errorMessage(j.getErrorMessage())
                .executionNode(j.getWorkerTaskId())
                .createdAt(j.getCreatedAt())
                .build();
    }

    private AdminPaymentResponse mapToAdminPayment(Payment p) {
        return AdminPaymentResponse.builder()
                .id(p.getId())
                .paymentId(p.getPaymentId())
                .userEmail(p.getUser() != null ? p.getUser().getEmail() : "user")
                .amountVnd(p.getAmountVnd())
                .credits(p.getCredits())
                .paymentMethod(p.getPaymentMethod())
                .transferContent(p.getTransferContent())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
