package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Credit;
import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.service.CreditService;
import com.hyperxconvert.api.service.PaymentService;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CreditService creditService;
    private final UserService userService;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        try {
            String paymentMethodStr = (String) request.get("paymentMethod");
            if (paymentMethodStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment method is required"
                ));
            }
            
            Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            
            Double amount = Double.parseDouble(request.get("amount").toString());
            String currency = (String) request.getOrDefault("currency", "USD");
            String description = (String) request.getOrDefault("description", "Payment for HyperXConvert");
            String successUrl = (String) request.getOrDefault("successUrl", "https://hyperxconvert.com/payment/success");
            String cancelUrl = (String) request.getOrDefault("cancelUrl", "https://hyperxconvert.com/payment/cancel");
            String webhookUrl = (String) request.getOrDefault("webhookUrl", "https://hyperxconvert.com/api/v1/payments/webhook");
            
            Map<String, Object> checkoutSession = paymentService.createCheckoutSession(
                    user, amount, currency, description, paymentMethod, successUrl, cancelUrl, webhookUrl);
            
            return ResponseEntity.ok(checkoutSession);
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

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestHeader("Stripe-Signature") String signature,
            @RequestBody String payload) {
        
        try {
            paymentService.handleStripeWebhook(payload, signature);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/plans")
    public ResponseEntity<?> getSubscriptionPlans() {
        List<Map<String, Object>> plans = paymentService.getSubscriptionPlans();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/buy-credits")
    public ResponseEntity<?> buyCredits(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request) {
        
        User user = userService.findByEmail(userDetails.getUsername());
        
        try {
            Integer amount = Integer.parseInt(request.get("amount").toString());
            if (amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Credit amount must be positive"
                ));
            }
            
            String paymentMethodStr = (String) request.get("paymentMethod");
            if (paymentMethodStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment method is required"
                ));
            }
            
            Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            
            // Calculate price based on credit amount
            double price = creditService.calculateCreditPrice(amount);
            
            // Create payment
            Payment payment = paymentService.createPayment(
                    user, price, "USD", "Credits purchase: " + amount + " credits", paymentMethod);
            
            // Add credits to user account
            Credit credit = creditService.addCredits(user, amount, payment.getId());
            
            return ResponseEntity.ok(Map.of(
                "payment", payment,
                "credits", credit,
                "totalCredits", creditService.getTotalActiveCredits(user)
            ));
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
}
