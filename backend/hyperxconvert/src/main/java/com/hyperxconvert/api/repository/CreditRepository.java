package com.hyperxconvert.api.repository;

import com.hyperxconvert.api.entity.Credit;
import com.hyperxconvert.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Credit entities
 */
@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {
    
    /**
     * Find credits by user
     *
     * @param user The user
     * @return The credits
     */
    List<Credit> findByUser(User user);
    
    /**
     * Find credits by user and status
     *
     * @param user The user
     * @param status The status
     * @return The credits
     */
    List<Credit> findByUserAndStatus(User user, Credit.CreditStatus status);
    
    /**
     * Get total active credits for a user
     *
     * @param user The user
     * @return The total active credits
     */
    @Query("SELECT SUM(c.amount) FROM Credit c WHERE c.user = :user AND c.status = 'ACTIVE'")
    Integer getTotalActiveCredits(@Param("user") User user);
    
    /**
     * Find credits by transaction ID
     *
     * @param transactionId The transaction ID
     * @return The credits
     */
    List<Credit> findByTransactionId(String transactionId);
}
