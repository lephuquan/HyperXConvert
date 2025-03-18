package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.ApiKey;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.ApiKeyRepository;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/developer")
@RequiredArgsConstructor
public class DeveloperController {

    private final ApiKeyRepository apiKeyRepository;
    private final UserService userService;

    @PostMapping("/api-keys")
    public ResponseEntity<?> createApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Map<String, Object> request) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        // Check if user already has active API keys
        List<ApiKey> activeKeys = apiKeyRepository.findByUserAndStatus(user, ApiKey.ApiKeyStatus.ACTIVE);
        if (activeKeys.size() >= 5) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "You can have a maximum of 5 active API keys"
            ));
        }
        
        // Generate a new API key
        String apiKeyString = generateApiKey();
        
        // Default rate limit based on subscription
        int rateLimit = 50; // Default
        if (user.getSubscriptions() != null && !user.getSubscriptions().isEmpty()) {
            // Find active subscription
            user.getSubscriptions().stream()
                    .filter(s -> s.isActive())
                    .findFirst()
                    .ifPresent(s -> {
                        // Use subscription rate limit
                    });
        }
        
        // Override rate limit if provided
        if (request != null && request.containsKey("rateLimit")) {
            try {
                rateLimit = Integer.parseInt(request.get("rateLimit").toString());
            } catch (NumberFormatException e) {
                // Ignore and use default
            }
        }
        
        // Create API key
        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .apiKey(apiKeyString)
                .status(ApiKey.ApiKeyStatus.ACTIVE)
                .rateLimitDaily(rateLimit)
                .createdAt(LocalDateTime.now())
                .build();
        
        apiKey = apiKeyRepository.save(apiKey);
        
        return ResponseEntity.ok(Map.of(
            "id", apiKey.getId(),
            "apiKey", apiKey.getApiKey(),
            "rateLimitDaily", apiKey.getRateLimitDaily(),
            "createdAt", apiKey.getCreatedAt()
        ));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<?> getApiKeys(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<ApiKey> apiKeys = apiKeyRepository.findByUser(user);
        return ResponseEntity.ok(apiKeys);
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<?> deleteApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElse(null);
        
        if (apiKey == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the API key belongs to the user
        if (!apiKey.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "You don't have permission to access this API key"
            ));
        }
        
        // Revoke the API key
        apiKey.setStatus(ApiKey.ApiKeyStatus.REVOKED);
        apiKeyRepository.save(apiKey);
        
        return ResponseEntity.ok(Map.of(
            "message", "API key revoked successfully"
        ));
    }
    
    private String generateApiKey() {
        return "hxc_" + UUID.randomUUID().toString().replace("-", "");
    }
}
