package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.ConversionRepository;
import com.hyperxconvert.api.repository.PaymentRepository;
import com.hyperxconvert.api.repository.UserRepository;
import com.hyperxconvert.api.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final ConversionRepository conversionRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<User> users = userRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users.getContent());
        response.put("currentPage", users.getNumber());
        response.put("totalItems", users.getTotalElements());
        response.put("totalPages", users.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversions")
    public ResponseEntity<?> getConversions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Conversion> conversions = conversionRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("conversions", conversions.getContent());
        response.put("currentPage", conversions.getNumber());
        response.put("totalItems", conversions.getTotalElements());
        response.put("totalPages", conversions.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Payment> payments = paymentRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("payments", payments.getContent());
        response.put("currentPage", payments.getNumber());
        response.put("totalItems", payments.getTotalElements());
        response.put("totalPages", payments.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> request) {
        String subject = (String) request.get("subject");
        String content = (String) request.get("content");
        List<String> emails = (List<String>) request.get("emails");
        
        if (subject == null || content == null || emails == null || emails.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Subject, content, and emails are required"
            ));
        }
        
        int sentCount = 0;
        for (String email : emails) {
            try {
                emailService.sendEmail(email, subject, content);
                sentCount++;
            } catch (Exception e) {
                // Continue sending to other emails even if one fails
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Notifications sent successfully",
            "sentCount", sentCount,
            "totalCount", emails.size()
        ));
    }
    
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        String status = request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Status is required"
            ));
        }
        
        User user = userRepository.findById(id)
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            user.setStatus(userStatus);
            userRepository.save(user);
            
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid status: " + status
            ));
        }
    }
}
