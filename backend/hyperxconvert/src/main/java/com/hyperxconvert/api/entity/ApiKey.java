package com.hyperxconvert.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_keys")
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(unique = true, nullable = false)
    private String apiKey;
    
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    @Enumerated(EnumType.STRING)
    private ApiKeyStatus status;
    
    private Integer rateLimitDaily;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum ApiKeyStatus {
        ACTIVE, REVOKED
    }
}
