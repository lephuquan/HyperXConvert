package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.SubscriptionRepository;
import com.hyperxconvert.api.service.SubscriptionService;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        String planTypeStr = request.get("planType");
        if (planTypeStr == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Plan type is required"
            ));
        }
        
        try {
            Subscription.PlanType planType = Subscription.PlanType.valueOf(planTypeStr.toUpperCase());
            Subscription subscription = subscriptionService.createSubscription(user, planType);
            
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid plan type: " + planTypeStr
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        Subscription subscription = subscriptionRepository.findById(id)
                .orElse(null);
        
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the subscription belongs to the user
        if (!subscription.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "You don't have permission to access this subscription"
            ));
        }
        
        try {
            // Update auto-renew setting if provided
            if (request.containsKey("autoRenew")) {
                Boolean autoRenew = (Boolean) request.get("autoRenew");
                subscription = subscriptionService.updateAutoRenew(subscription, autoRenew);
            }
            
            // Update plan type if provided
            if (request.containsKey("planType")) {
                String planTypeStr = (String) request.get("planType");
                Subscription.PlanType planType = Subscription.PlanType.valueOf(planTypeStr.toUpperCase());
                subscription = subscriptionService.upgradePlan(subscription, planType);
            }
            
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid request: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        Subscription subscription = subscriptionRepository.findById(id)
                .orElse(null);
        
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the subscription belongs to the user
        if (!subscription.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "You don't have permission to access this subscription"
            ));
        }
        
        try {
            subscriptionService.cancelSubscription(subscription);
            
            return ResponseEntity.ok(Map.of(
                "message", "Subscription cancelled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
