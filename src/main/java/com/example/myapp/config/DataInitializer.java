package com.example.myapp.config;

import com.example.myapp.features.auth.User;
import com.example.myapp.features.auth.UserRepository;
import com.example.myapp.features.gateway.*;
import com.example.myapp.features.wallet.WalletService;
import com.example.myapp.features.workspace.PromptTemplate;
import com.example.myapp.features.workspace.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AiModelRepository aiModelRepository;
    private final AiDispatchGroupRepository dispatchGroupRepository;
    private final AiUpstreamEndpointRepository upstreamEndpointRepository;
    private final AiCapabilityRepository capabilityRepository;
    private final AiModelPricingRepository pricingRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("admin@leonix.ai")) {
            User admin = userRepository.save(User.builder()
                    .email("admin@leonix.ai")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("LeoNix SuperAdmin")
                    .role("ADMIN")
                    .status("ACTIVE")
                    .build());
            walletService.getOrCreateWallet(admin);
            log.info("Initialized default ADMIN user: admin@leonix.ai / admin123");
        }

        if (aiModelRepository.count() > 0) {
            log.info("Catalog data already exists. Skipping data initialization.");
            return;
        }

        log.info("Initializing LeoNix AI models, dispatch groups, and sample templates...");

        // 1. Dispatch Groups
        AiDispatchGroup dgGrok = dispatchGroupRepository.save(AiDispatchGroup.builder()
                .groupCode("dg-xai-grok")
                .name("XAI Grok Video Cluster")
                .strategy("PRIORITY_FAILOVER")
                .failoverStatusCodes("429,500,502,503,504")
                .maxRetriesPerUpstream(2)
                .timeoutMs(45000)
                .build());

        AiDispatchGroup dgVeo = dispatchGroupRepository.save(AiDispatchGroup.builder()
                .groupCode("dg-google-veo")
                .name("Google Veo 2 Cluster")
                .strategy("PRIORITY_FAILOVER")
                .failoverStatusCodes("429,500,502,503,504")
                .maxRetriesPerUpstream(2)
                .timeoutMs(60000)
                .build());

        AiDispatchGroup dgKling = dispatchGroupRepository.save(AiDispatchGroup.builder()
                .groupCode("dg-kling-video")
                .name("Kling Video Global Cluster")
                .strategy("PRIORITY_FAILOVER")
                .failoverStatusCodes("429,500,502,503,504")
                .maxRetriesPerUpstream(2)
                .timeoutMs(60000)
                .build());

        AiDispatchGroup dgFlux = dispatchGroupRepository.save(AiDispatchGroup.builder()
                .groupCode("dg-flux-image")
                .name("FLUX.1 Schnell Cluster")
                .strategy("ROUND_ROBIN")
                .failoverStatusCodes("429,500,502,503,504")
                .maxRetriesPerUpstream(1)
                .timeoutMs(25000)
                .build());

        AiDispatchGroup dgVoice = dispatchGroupRepository.save(AiDispatchGroup.builder()
                .groupCode("dg-elevenlabs-voice")
                .name("ElevenLabs Voice Cluster")
                .strategy("PRIORITY_FAILOVER")
                .failoverStatusCodes("429,500,502,503,504")
                .maxRetriesPerUpstream(1)
                .timeoutMs(20000)
                .build());

        // 2. Upstream Endpoints (cURL configurations with fallback chains)
        upstreamEndpointRepository.save(AiUpstreamEndpoint.builder()
                .dispatchGroup(dgGrok)
                .name("Grok Primary API Node US")
                .priority(1)
                .weight(100)
                .endpointUrl("https://api.x.ai/v1/videos/generations")
                .httpMethod("POST")
                .headersTemplate("{\"Authorization\": \"Bearer {{API_KEY}}\", \"Content-Type\": \"application/json\"}")
                .apiKeyEncrypted("mock-xai-key-prod-01")
                .requestBodyTemplate("{\"prompt\": \"{{PROMPT}}\", \"aspect_ratio\": \"{{ASPECT_RATIO}}\", \"duration\": {{DURATION}}}")
                .responseMapping("{\"job_id\": \"id\", \"status\": \"status\", \"output_url\": \"data[0].url\"}")
                .isActive(true)
                .healthStatus("HEALTHY")
                .build());

        upstreamEndpointRepository.save(AiUpstreamEndpoint.builder()
                .dispatchGroup(dgGrok)
                .name("Grok Backup API Node EU (Fallback)")
                .priority(2)
                .weight(50)
                .endpointUrl("https://eu-api.x.ai/v1/videos/generations")
                .httpMethod("POST")
                .headersTemplate("{\"Authorization\": \"Bearer {{API_KEY}}\", \"Content-Type\": \"application/json\"}")
                .apiKeyEncrypted("mock-xai-key-eu-backup")
                .requestBodyTemplate("{\"prompt\": \"{{PROMPT}}\", \"aspect_ratio\": \"{{ASPECT_RATIO}}\", \"duration\": {{DURATION}}}")
                .responseMapping("{\"job_id\": \"id\", \"status\": \"status\", \"output_url\": \"data[0].url\"}")
                .isActive(true)
                .healthStatus("HEALTHY")
                .build());

        upstreamEndpointRepository.save(AiUpstreamEndpoint.builder()
                .dispatchGroup(dgVeo)
                .name("Google Vertex Veo-2 Endpoint")
                .priority(1)
                .weight(100)
                .endpointUrl("https://us-central1-aiplatform.googleapis.com/v1/projects/leonix/models/veo-2:predict")
                .httpMethod("POST")
                .headersTemplate("{\"Authorization\": \"Bearer {{API_KEY}}\", \"Content-Type\": \"application/json\"}")
                .apiKeyEncrypted("mock-vertex-ai-key")
                .requestBodyTemplate("{\"instances\": [{\"prompt\": \"{{PROMPT}}\", \"duration\": \"{{DURATION}}\"}]}")
                .responseMapping("{\"job_id\": \"name\", \"status\": \"state\", \"output_url\": \"predictions[0].bytesBase64Encoded\"}")
                .isActive(true)
                .healthStatus("HEALTHY")
                .build());

        upstreamEndpointRepository.save(AiUpstreamEndpoint.builder()
                .dispatchGroup(dgFlux)
                .name("FLUX Image Node 1")
                .priority(1)
                .weight(100)
                .endpointUrl("https://api.bfl.ml/v1/flux-pro-1.1")
                .httpMethod("POST")
                .headersTemplate("{\"x-key\": \"{{API_KEY}}\", \"Content-Type\": \"application/json\"}")
                .apiKeyEncrypted("mock-bfl-key")
                .requestBodyTemplate("{\"prompt\": \"{{PROMPT}}\", \"width\": 1024, \"height\": 768}")
                .responseMapping("{\"job_id\": \"id\", \"status\": \"status\", \"output_url\": \"result.sample\"}")
                .isActive(true)
                .healthStatus("HEALTHY")
                .build());

        upstreamEndpointRepository.save(AiUpstreamEndpoint.builder()
                .dispatchGroup(dgVoice)
                .name("ElevenLabs Text-to-Speech Node")
                .priority(1)
                .weight(100)
                .endpointUrl("https://api.elevenlabs.io/v1/text-to-speech/21m00Tcm4TlvDq8ikWAM")
                .httpMethod("POST")
                .headersTemplate("{\"xi-api-key\": \"{{API_KEY}}\", \"Content-Type\": \"application/json\"}")
                .apiKeyEncrypted("mock-elevenlabs-key")
                .requestBodyTemplate("{\"text\": \"{{PROMPT}}\", \"model_id\": \"eleven_multilingual_v2\"}")
                .responseMapping("{\"output_url\": \"audio_url\"}")
                .isActive(true)
                .healthStatus("HEALTHY")
                .build());

        // 3. Models
        AiModel modelGrok = aiModelRepository.save(AiModel.builder()
                .id("xai/grok-imagine-video")
                .name("Grok Imagine Video")
                .provider("xai")
                .serviceType("VIDEO")
                .capabilities("{\"aspect_ratios\": [\"16:9\", \"9:16\", \"1:1\"], \"resolutions\": [\"720p\", \"1080p\"], \"durations\": [\"5s\", \"10s\"]}")
                .isActive(true)
                .displayOrder(1)
                .build());

        AiModel modelVeo = aiModelRepository.save(AiModel.builder()
                .id("google/veo-2")
                .name("Google Veo 2.0 Ultra")
                .provider("google")
                .serviceType("VIDEO")
                .capabilities("{\"aspect_ratios\": [\"16:9\", \"9:16\", \"1:1\"], \"resolutions\": [\"720p\", \"1080p\"], \"durations\": [\"5s\", \"10s\"]}")
                .isActive(true)
                .displayOrder(2)
                .build());

        AiModel modelKling = aiModelRepository.save(AiModel.builder()
                .id("kling/v1-standard")
                .name("Kling AI v1.5 Standard")
                .provider("kling")
                .serviceType("VIDEO")
                .capabilities("{\"aspect_ratios\": [\"16:9\", \"9:16\", \"1:1\"], \"resolutions\": [\"720p\", \"1080p\"], \"durations\": [\"5s\", \"10s\"]}")
                .isActive(true)
                .displayOrder(3)
                .build());

        AiModel modelFlux = aiModelRepository.save(AiModel.builder()
                .id("bfl/flux-1-schnell")
                .name("FLUX.1 Schnell")
                .provider("bfl")
                .serviceType("IMAGE")
                .capabilities("{\"aspect_ratios\": [\"16:9\", \"9:16\", \"1:1\", \"4:3\"], \"resolutions\": [\"1024x1024\"]}")
                .isActive(true)
                .displayOrder(4)
                .build());

        AiModel modelVoice = aiModelRepository.save(AiModel.builder()
                .id("elevenlabs/multilingual-v2")
                .name("ElevenLabs Multilingual v2")
                .provider("elevenlabs")
                .serviceType("VOICE")
                .capabilities("{\"voices\": [\"Rachel\", \"Domi\", \"Bella\", \"Antoni\"]}")
                .isActive(true)
                .displayOrder(5)
                .build());

        // 4. Capabilities mapping
        capabilityRepository.save(AiCapability.builder()
                .model(modelGrok)
                .capabilityCode("t2v")
                .name("Text to Video")
                .supportedResolutions("720p,1080p")
                .supportedDurations("5s,10s")
                .supportedAspectRatios("16:9,9:16,1:1")
                .dispatchGroup(dgGrok)
                .isActive(true)
                .build());

        capabilityRepository.save(AiCapability.builder()
                .model(modelGrok)
                .capabilityCode("i2v")
                .name("Image to Video")
                .supportedResolutions("720p,1080p")
                .supportedDurations("5s,10s")
                .supportedAspectRatios("16:9,9:16,1:1")
                .dispatchGroup(dgGrok)
                .isActive(true)
                .build());

        capabilityRepository.save(AiCapability.builder()
                .model(modelVeo)
                .capabilityCode("t2v")
                .name("Text to Video")
                .supportedResolutions("720p,1080p")
                .supportedDurations("5s,10s")
                .supportedAspectRatios("16:9,9:16,1:1")
                .dispatchGroup(dgVeo)
                .isActive(true)
                .build());

        capabilityRepository.save(AiCapability.builder()
                .model(modelFlux)
                .capabilityCode("t2i")
                .name("Text to Image")
                .supportedResolutions("1024x1024")
                .supportedAspectRatios("16:9,9:16,1:1,4:3")
                .dispatchGroup(dgFlux)
                .isActive(true)
                .build());

        capabilityRepository.save(AiCapability.builder()
                .model(modelVoice)
                .capabilityCode("tts")
                .name("Text to Speech")
                .dispatchGroup(dgVoice)
                .isActive(true)
                .build());

        // 5. Model Pricing
        pricingRepository.save(AiModelPricing.builder()
                .model(modelGrok)
                .capabilityCode("t2v")
                .resolution("720p")
                .duration("5s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("15.00"))
                .costPrice(new BigDecimal("8.00"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelGrok)
                .capabilityCode("t2v")
                .resolution("1080p")
                .duration("5s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("30.00"))
                .costPrice(new BigDecimal("18.00"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelGrok)
                .capabilityCode("t2v")
                .resolution("720p")
                .duration("10s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("28.00"))
                .costPrice(new BigDecimal("15.00"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelGrok)
                .capabilityCode("t2v")
                .resolution("1080p")
                .duration("10s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("55.00"))
                .costPrice(new BigDecimal("32.00"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelVeo)
                .capabilityCode("t2v")
                .resolution("720p")
                .duration("5s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("20.00"))
                .costPrice(new BigDecimal("12.00"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelFlux)
                .capabilityCode("t2i")
                .resolution("1024x1024")
                .duration("1s")
                .priceUnit("PER_CALL")
                .sellPrice(new BigDecimal("5.00"))
                .costPrice(new BigDecimal("2.50"))
                .build());

        pricingRepository.save(AiModelPricing.builder()
                .model(modelVoice)
                .capabilityCode("tts")
                .priceUnit("PER_1K_CHARS")
                .sellPrice(new BigDecimal("2.00"))
                .costPrice(new BigDecimal("1.00"))
                .build());

        // 6. Sample Prompt Templates
        promptTemplateRepository.save(PromptTemplate.builder()
                .title("Cyberpunk City Night Flythrough")
                .category("CINEMATIC")
                .serviceType("VIDEO")
                .promptVi("Góc quay drone lướt qua thành phố cyberpunk rực rỡ ánh đèn neon tím và xanh, mưa rơi lấp lánh phản chiếu mặt đường ướt.")
                .promptEn("Cinematic drone flythrough in a hyperrealistic cyberpunk metropolis, illuminated by vibrant purple and neon blue signage, puddles reflecting city lights in slow motion.")
                .previewMediaUrl("https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&auto=format&fit=crop")
                .tags("cyberpunk,drone,cinematic,neon")
                .isSystem(true)
                .likesCount(128)
                .build());

        promptTemplateRepository.save(PromptTemplate.builder()
                .title("Fashion Studio Commercial Close-up")
                .category("FASHION")
                .serviceType("VIDEO")
                .promptVi("Người mẫu thời trang haute couture thần thái tự tin, ánh sáng studio mềm mại, máy quay quay chậm 60fps làm nổi bật chi tiết lụa cao cấp.")
                .promptEn("High fashion model posing in ultra-soft studio lighting, 8k resolution, slow motion 60fps closeup focusing on luxurious silk fabric texture.")
                .previewMediaUrl("https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=800&auto=format&fit=crop")
                .tags("fashion,model,studio,commercial")
                .isSystem(true)
                .likesCount(95)
                .build());

        promptTemplateRepository.save(PromptTemplate.builder()
                .title("TikTok Viral Product Unboxing")
                .category("TIKTOK_REELS")
                .serviceType("VIDEO")
                .promptVi("Tay mở hộp sản phẩm công nghệ cao cấp cực mượt, góc nhìn người thứ nhất POV, năng động, ánh sáng ấm hiện đại.")
                .promptEn("Viral TikTok POV opening premium sleek tech package with crisp smooth unboxing movement, dynamic studio lighting, satisfying product reveal.")
                .previewMediaUrl("https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=800&auto=format&fit=crop")
                .tags("tiktok,product,unboxing,tech")
                .isSystem(true)
                .likesCount(210)
                .build());

        promptTemplateRepository.save(PromptTemplate.builder()
                .title("Anime Fantasy Sakura Forest")
                .category("ANIME")
                .serviceType("IMAGE")
                .promptVi("Phong cảnh rừng hoa anh đào nở rộ phong cách Makoto Shinkai, mây bồng bềnh và ánh nắng hoàng hôn vàng ấm.")
                .promptEn("Makoto Shinkai anime style blossom sakura forest, gentle petals blowing in wind, golden hour sunlight streaming through clouds, 4k wallpaper.")
                .previewMediaUrl("https://images.unsplash.com/photo-1522383225653-ed111181a951?w=800&auto=format&fit=crop")
                .tags("anime,sakura,wallpaper,makotoshinkai")
                .isSystem(true)
                .likesCount(340)
                .build());

        log.info("Initialized LeoNix AI models, dispatch groups, and sample templates successfully.");
    }
}
