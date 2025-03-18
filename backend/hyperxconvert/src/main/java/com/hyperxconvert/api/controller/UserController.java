package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.Credit;
import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.ConversionRepository;
import com.hyperxconvert.api.repository.CreditRepository;
import com.hyperxconvert.api.repository.PaymentRepository;
import com.hyperxconvert.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ConversionRepository conversionRepository;
    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal UserDetails userDetails, 
                                       @RequestBody Map<String, String> updates) {
        User user = userService.findByEmail(userDetails.getUsername());
        
        if (updates.containsKey("fullName")) {
            user.setFullName(updates.get("fullName"));
        }
        
        // Password update would require old password verification and should be handled separately
        
        User updatedUser = userService.updateUser(user);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/me/conversions")
    public ResponseEntity<?> getUserConversions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Conversion> conversions = conversionRepository.findByUserOrderByCreatedAtDesc(user);
        return ResponseEntity.ok(conversions);
    }

    @GetMapping("/me/payments")
    public ResponseEntity<?> getUserPayments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Payment> payments = paymentRepository.findByUser(user);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/me/credits")
    public ResponseEntity<?> getUserCredits(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Credit> credits = creditRepository.findByUser(user);
        Integer totalActiveCredits = creditRepository.getTotalActiveCredits(user);
        
        return ResponseEntity.ok(Map.of(
            "credits", credits,
            "totalActiveCredits", totalActiveCredits != null ? totalActiveCredits : 0
        ));
    }
}
