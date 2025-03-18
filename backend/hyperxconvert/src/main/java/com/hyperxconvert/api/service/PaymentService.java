package com.hyperxconvert.api.service;

import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CreditService creditService;
    private final SubscriptionService subscriptionService;
    
    @Value("${stripe.webhook.secret:stripe-webhook-secret}")
    private String stripeWebhookSecret;

    @Transactional
    public Payment createPayment(User user, double amount, String currency, String description, Payment.PaymentMethod paymentMethod) {
        Payment payment = Payment.builder()
                .user(user)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .status(Payment.PaymentStatus.COMPLETED) // For simplicity, we're setting it to COMPLETED directly
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        
        return paymentRepository.save(payment);
    }
    
    @Transactional
    public Map<String, Object> createCheckoutSession(
            User user, double amount, String currency, String description,
            Payment.PaymentMethod paymentMethod, String successUrl, String cancelUrl, String webhookUrl) {
        
        // In a real implementation, this would integrate with the payment gateway API
        // For now, we'll just create a payment record and return a mock checkout URL
        
        Payment payment = Payment.builder()
                .user(user)
                .amount(amount)
                .currency(currency)
                .paymentMethod(paymentMethod)
                .status(Payment.PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        
        payment = paymentRepository.save(payment);
        
        // Mock checkout URL
        String checkoutUrl = "https://hyperxconvert.com/checkout/" + payment.getId();
        
        return Map.of(
            "paymentId", payment.getId(),
            "checkoutUrl", checkoutUrl
        );
    }
    
    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        // In a real implementation, this would verify the signature and process the webhook
        // For now, we'll just log the webhook and update a mock payment
        
        System.out.println("Received Stripe webhook: " + payload);
        System.out.println("Signature: " + signature);
        
        // Mock payment update
        // In a real implementation, we would extract the payment ID from the webhook payload
        // and update the corresponding payment record
        
        // For now, we'll just update the most recent pending payment
        List<Payment> pendingPayments = paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
        if (!pendingPayments.isEmpty()) {
            Payment payment = pendingPayments.get(0);
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            
            // Process the payment based on its purpose
            processCompletedPayment(payment);
        }
    }
    
    private void processCompletedPayment(Payment payment) {
        // Determine what the payment was for and process accordingly
        // For now, we'll just add credits to the user's account
        
        // Add 100 credits for every $10 spent
        int creditAmount = (int) (payment.getAmount() / 0.1);
        creditService.addCredits(payment.getUser(), creditAmount, payment.getId());
    }
    
    public List<Map<String, Object>> getSubscriptionPlans() {
        List<Map<String, Object>> plans = new ArrayList<>();
        
        // Basic plan
        plans.add(Map.of(
            "id", "basic",
            "name", "Basic",
            "price", 9.99,
            "currency", "USD",
            "features", List.of(
                "10 MB max file size",
                "7 days file retention",
                "50 API calls per day"
            )
        ));
        
        // Pro plan
        plans.add(Map.of(
            "id", "pro",
            "name", "Pro",
            "price", 19.99,
            "currency", "USD",
            "features", List.of(
                "50 MB max file size",
                "14 days file retention",
                "200 API calls per day"
            )
        ));
        
        // Ultimate plan
        plans.add(Map.of(
            "id", "ultimate",
            "name", "Ultimate",
            "price", 49.99,
            "currency", "USD",
            "features", List.of(
                "100 MB max file size",
                "30 days file retention",
                "500 API calls per day"
            )
        ));
        
        return plans;
    }
}
