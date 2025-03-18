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
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private PlanType planType;
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    
    private String paymentId;
    private Boolean autoRenew;
    
    public enum PlanType {
        BASIC, PRO, ULTIMATE
    }
    
    public enum SubscriptionStatus {
        ACTIVE, CANCELLED, EXPIRED
    }
    
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               endDate != null && 
               endDate.isAfter(LocalDateTime.now());
    }
    
    public int getRateLimit() {
        switch (planType) {
            case BASIC:
                return 50;
            case PRO:
                return 200;
            case ULTIMATE:
                return 500;
            default:
                return 10;
        }
    }
    
    public int getMaxFileSize() {
        switch (planType) {
            case BASIC:
                return 10; // 10 MB
            case PRO:
                return 50; // 50 MB
            case ULTIMATE:
                return 100; // 100 MB
            default:
                return 5; // 5 MB
        }
    }
    
    public int getRetentionDays() {
        switch (planType) {
            case BASIC:
                return 7;
            case PRO:
                return 14;
            case ULTIMATE:
                return 30;
            default:
                return 3;
        }
    }
}
