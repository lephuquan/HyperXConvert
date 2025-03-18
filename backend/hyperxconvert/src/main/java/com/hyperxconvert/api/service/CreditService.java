package com.hyperxconvert.api.service;

import com.hyperxconvert.api.entity.Credit;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.CreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;

    @Transactional
    public Credit addCredits(User user, int amount, Long transactionId) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        
        Credit credit = Credit.builder()
                .user(user)
                .amount(amount)
                .status(Credit.CreditStatus.ACTIVE)
                .transactionId(transactionId != null ? transactionId.toString() : null)
                .createdAt(LocalDateTime.now())
                .build();
        
        return creditRepository.save(credit);
    }
    
    @Transactional(readOnly = true)
    public Integer getTotalActiveCredits(User user) {
        return creditRepository.getTotalActiveCredits(user);
    }
    
    public double calculateCreditPrice(int amount) {
        // Base price: $0.10 per credit
        double basePrice = amount * 0.10;
        
        // Volume discounts
        if (amount >= 1000) {
            // 20% discount for 1000+ credits
            return basePrice * 0.8;
        } else if (amount >= 500) {
            // 15% discount for 500+ credits
            return basePrice * 0.85;
        } else if (amount >= 100) {
            // 10% discount for 100+ credits
            return basePrice * 0.9;
        }
        
        return basePrice;
    }
    
    @Transactional
    public void useCredits(User user, int amount) {
        Integer availableCredits = getTotalActiveCredits(user);
        
        if (availableCredits == null || availableCredits < amount) {
            throw new RuntimeException("Not enough credits. Required: " + amount + ", Available: " + 
                    (availableCredits != null ? availableCredits : 0));
        }
        
        // Create a negative credit entry to represent usage
        Credit credit = Credit.builder()
                .user(user)
                .amount(-amount)
                .status(Credit.CreditStatus.USED)
                .createdAt(LocalDateTime.now())
                .build();
        
        creditRepository.save(credit);
    }
}
