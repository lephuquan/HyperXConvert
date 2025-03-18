package com.hyperxconvert.api.repository;

import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    
    Optional<Subscription> findByUserAndStatus(User user, Subscription.SubscriptionStatus status);
    
    List<Subscription> findByEndDateBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT s FROM Subscription s WHERE s.user = ?1 AND s.status = 'ACTIVE' AND s.endDate > CURRENT_TIMESTAMP")
    Optional<Subscription> findActiveSubscription(User user);
}
