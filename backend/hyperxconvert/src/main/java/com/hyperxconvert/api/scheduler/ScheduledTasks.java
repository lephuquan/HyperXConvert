package com.hyperxconvert.api.scheduler;

import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.repository.ConversionRepository;
import com.hyperxconvert.api.repository.SubscriptionRepository;
import com.hyperxconvert.api.service.EmailService;
import com.hyperxconvert.api.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasks {

    private final ConversionRepository conversionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final S3StorageService s3StorageService;
    private final EmailService emailService;

    /**
     * Cleanup expired files daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredFiles() {
        log.info("Starting expired files cleanup task");
        
        List<Conversion> expiredConversions = conversionRepository.findByExpiryDateBeforeAndStatus(
                LocalDateTime.now(), Conversion.ConversionStatus.COMPLETED);
        
        log.info("Found {} expired conversions to cleanup", expiredConversions.size());
        
        for (Conversion conversion : expiredConversions) {
            try {
                // Delete file from S3
                if (conversion.getS3ConvertedPath() != null) {
                    s3StorageService.deleteFile(conversion.getS3ConvertedPath());
                }
                
                // Update status
                conversion.setStatus(Conversion.ConversionStatus.EXPIRED);
                conversionRepository.save(conversion);
                
                log.info("Cleaned up expired conversion: {}", conversion.getId());
            } catch (Exception e) {
                log.error("Failed to cleanup expired file: " + conversion.getId(), e);
            }
        }
        
        log.info("Completed expired files cleanup task");
    }

    /**
     * Check for expiring subscriptions daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(readOnly = true)
    public void checkSubscriptions() {
        log.info("Starting subscription check task");
        
        // Find subscriptions expiring in the next 3 days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);
        
        List<Subscription> expiringSubscriptions = subscriptionRepository.findByEndDateBetween(
                now, threeDaysLater);
        
        log.info("Found {} subscriptions expiring soon", expiringSubscriptions.size());
        
        for (Subscription subscription : expiringSubscriptions) {
            try {
                // Send email notification
                emailService.sendSubscriptionExpiringEmail(
                        subscription.getUser().getEmail(),
                        subscription.getUser().getFullName(),
                        subscription.getPlanType().toString(),
                        subscription.getEndDate());
                
                log.info("Sent expiration notification for subscription: {}", subscription.getId());
            } catch (Exception e) {
                log.error("Failed to send subscription expiry notification for ID: " + subscription.getId(), e);
            }
        }
        
        log.info("Completed subscription check task");
    }
}
