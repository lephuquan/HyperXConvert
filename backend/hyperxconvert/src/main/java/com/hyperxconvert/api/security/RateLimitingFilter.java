package com.hyperxconvert.api.security;

import com.hyperxconvert.api.entity.ApiKey;
import com.hyperxconvert.api.repository.ApiKeyRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip rate limiting for non-API endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get API key from header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            // No API key provided, proceed without rate limiting
            filterChain.doFilter(request, response);
            return;
        }
        
        // Validate API key
        Optional<ApiKey> apiKeyEntity = apiKeyRepository.findByApiKey(apiKey);
        if (apiKeyEntity.isEmpty() || apiKeyEntity.get().getStatus() != ApiKey.ApiKeyStatus.ACTIVE) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid API key");
            return;
        }
        
        // Update last used timestamp
        ApiKey apiKeyObj = apiKeyEntity.get();
        apiKeyObj.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKeyObj);
        
        // Get or create rate limit bucket
        Bucket bucket = buckets.computeIfAbsent(apiKey, k -> createNewBucket(apiKeyObj));
        
        // Check if request is allowed
        if (bucket.tryConsume(1)) {
            // Request is allowed, proceed
            filterChain.doFilter(request, response);
        } else {
            // Request is rate limited
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            log.warn("Rate limit exceeded for API key: {}", apiKey);
        }
    }
    
    private Bucket createNewBucket(ApiKey apiKey) {
        long rateLimit = apiKey.getRateLimitDaily();
        Bandwidth limit = Bandwidth.classic(rateLimit, Refill.intervally(rateLimit, Duration.ofDays(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
