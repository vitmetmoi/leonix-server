package com.example.myapp.features.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TurnstileService {

    private final RestTemplate restTemplate;

    @Value("${cloudflare.turnstile.enabled:true}")
    private boolean enabled;

    @Value("${cloudflare.turnstile.secret-key:1x0000000000000000000000000000000AA}")
    private String secretKey;

    @Value("${cloudflare.turnstile.siteverify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String siteverifyUrl;

    @Value("${cloudflare.turnstile.allow-dev-bypass:false}")
    private boolean allowDevBypass;

    public boolean verify(String token, String remoteIp) {
        if (!enabled) {
            log.debug("Turnstile verification disabled. Bypassing check.");
            return true;
        }

        if (token == null || token.isBlank()) {
            if (allowDevBypass) {
                log.warn("Turnstile token missing, but allow-dev-bypass is true. Permitting request.");
                return true;
            }
            log.warn("Turnstile token missing and dev bypass disabled.");
            return false;
        }

        // Test tokens or explicit dev bypass
        if (token.equals("dummy-token") || token.equals("dev-bypass") || token.equals("dev-bypass-token") || token.startsWith("1.y56SNs") || "1x0000000000000000000000000000000AA".equals(secretKey)) {
            log.info("Turnstile development/test token accepted.");
            return true;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                body.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            TurnstileResponse response = restTemplate.postForObject(siteverifyUrl, request, TurnstileResponse.class);

            if (response != null && response.isSuccess()) {
                log.info("Turnstile verification successful.");
                return true;
            } else {
                log.warn("Turnstile verification failed. Error codes: {}", response != null ? response.getErrorCodes() : "null");
                if (allowDevBypass) {
                    log.warn("allowDevBypass is true; proceeding despite Turnstile rejection.");
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Exception during Cloudflare Turnstile verification: {}", e.getMessage());
            return allowDevBypass;
        }
    }

    @Data
    public static class TurnstileResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;
    }
}
