package com.hyperxconvert.api.repository;

import com.hyperxconvert.api.entity.Payment;
import com.hyperxconvert.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser(User user);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    List<Payment> findByUserAndStatus(User user, Payment.PaymentStatus status);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
