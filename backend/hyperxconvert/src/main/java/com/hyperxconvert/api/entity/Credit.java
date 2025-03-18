package com.hyperxconvert.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity for credits
 */
@Entity
@Table(name = "credits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credit {

    /**
     * Credit status
     */
    public enum CreditStatus {
        ACTIVE,    // Credits that are available for use
        USED,      // Credits that have been used
        EXPIRED    // Credits that have expired
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Amount of credits (positive for additions, negative for usage)
     */
    @Column(nullable = false)
    private Integer amount;

    /**
     * Status of the credits
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditStatus status;

    /**
     * Transaction ID for credits purchased through payment
     */
    @Column(name = "transaction_id")
    private String transactionId;

    /**
     * When the credits were created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the credits expire (if applicable)
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Notes about the credits (e.g., reason for addition or usage)
     */
    @Column(length = 500)
    private String notes;
}
