package com.hyperxconvert.api.repository;

import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Conversion entities
 */
@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {
    
    /**
     * Find conversions by user
     *
     * @param user The user
     * @return The conversions
     */
    List<Conversion> findByUser(User user);
    
    /**
     * Find conversions by user with pagination
     *
     * @param user The user
     * @param pageable The pagination information
     * @return The conversions
     */
    Page<Conversion> findByUser(User user, Pageable pageable);
    
    /**
     * Find conversions by expiry date before and status
     *
     * @param expiryDate The expiry date
     * @param status The status
     * @return The conversions
     */
    List<Conversion> findByExpiryDateBeforeAndStatus(LocalDateTime expiryDate, Conversion.ConversionStatus status);
    
    /**
     * Find conversions by status
     *
     * @param status The status
     * @return The conversions
     */
    List<Conversion> findByStatus(Conversion.ConversionStatus status);
}
