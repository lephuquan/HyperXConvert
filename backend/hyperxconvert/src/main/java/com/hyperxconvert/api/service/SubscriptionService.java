package com.hyperxconvert.api.service;

import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.PaymentRepository;
import com.hyperxconvert.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Subscription createSubscription(User user, Subscription.PlanType planType) {
        // Check if user already has an active subscription
        subscriptionRepository.findActiveSubscription(user)
                .ifPresent(activeSubscription -> {
                    throw new RuntimeException("User already has an active subscription");
                });
        
        // Create a new subscription
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(planType)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30)) // 30-day subscription
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .autoRenew(false)
                .build();
        
        // Create a payment record
        Payment payment = Payment.builder()
                .user(user)
                .amount(getPlanPrice(planType))
                .currency("USD")
                .paymentMethod(Payment.PaymentMethod.STRIPE) // Default to Stripe
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);
        
        // Link payment to subscription
        subscription.setPaymentId(payment.getId().toString());
        
        return subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public Subscription updateAutoRenew(Subscription subscription, boolean autoRenew) {
        subscription.setAutoRenew(autoRenew);
        return subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public Subscription upgradePlan(Subscription subscription, Subscription.PlanType newPlanType) {
        // Check if the new plan is an upgrade
        if (getPlanPrice(newPlanType) <= getPlanPrice(subscription.getPlanType())) {
            throw new RuntimeException("New plan must be an upgrade");
        }
        
        // Calculate price difference
        double priceDifference = getPlanPrice(newPlanType) - getPlanPrice(subscription.getPlanType());
        
        // Create a payment record for the upgrade
        Payment payment = Payment.builder()
                .user(subscription.getUser())
                .amount(priceDifference)
                .currency("USD")
                .paymentMethod(Payment.PaymentMethod.STRIPE) // Default to Stripe
                .status(Payment.PaymentStatus.COMPLETED)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        
        paymentRepository.save(payment);
        
        // Update subscription
        subscription.setPlanType(newPlanType);
        subscription.setPaymentId(payment.getId().toString());
        
        return subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public void cancelSubscription(Subscription subscription) {
        if (subscription.getStatus() != Subscription.SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Subscription is not active");
        }
        
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        
        subscriptionRepository.save(subscription);
    }
    
    private double getPlanPrice(Subscription.PlanType planType) {
        switch (planType) {
            case BASIC:
                return 9.99;
            case PRO:
                return 19.99;
            case ULTIMATE:
                return 49.99;
            default:
                return 0.0;
        }
    }
}
