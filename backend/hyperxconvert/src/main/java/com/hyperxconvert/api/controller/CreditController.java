package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Credit;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.CreditRepository;
import com.hyperxconvert.api.service.CreditService;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for credit-related endpoints
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final CreditRepository creditRepository;
    private final UserService userService;

    /**
     * Get credits for the current user
     *
     * @param userDetails The authenticated user
     * @return The credits
     */
    @GetMapping
    public ResponseEntity<?> getCredits(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Credit> credits = creditRepository.findByUser(user);
        
        Integer totalActiveCredits = creditService.getTotalActiveCredits(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("credits", credits);
        response.put("totalActiveCredits", totalActiveCredits != null ? totalActiveCredits : 0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get credit packages
     *
     * @return The credit packages
     */
    @GetMapping("/packages")
    public ResponseEntity<?> getCreditPackages() {
        // Define credit packages with pricing
        List<Map<String, Object>> packages = List.of(
                Map.of(
                        "id", "basic",
                        "name", "Basic",
                        "credits", 100,
                        "price", creditService.calculateCreditPrice(100),
                        "description", "Basic package for occasional use"
                ),
                Map.of(
                        "id", "standard",
                        "name", "Standard",
                        "credits", 500,
                        "price", creditService.calculateCreditPrice(500),
                        "description", "Standard package for regular use"
                ),
                Map.of(
                        "id", "premium",
                        "name", "Premium",
                        "credits", 1000,
                        "price", creditService.calculateCreditPrice(1000),
                        "description", "Premium package for heavy use"
                ),
                Map.of(
                        "id", "enterprise",
                        "name", "Enterprise",
                        "credits", 5000,
                        "price", creditService.calculateCreditPrice(5000),
                        "description", "Enterprise package for business use"
                )
        );
        
        return ResponseEntity.ok(packages);
    }

    /**
     * Calculate credit price
     *
     * @param request The request containing the credit amount
     * @return The price
     */
    @PostMapping("/calculate-price")
    public ResponseEntity<?> calculatePrice(@RequestBody Map<String, Object> request) {
        Integer amount = (Integer) request.get("amount");
        
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid credit amount"
            ));
        }
        
        double price = creditService.calculateCreditPrice(amount);
        
        return ResponseEntity.ok(Map.of(
                "amount", amount,
                "price", price
        ));
    }
}
