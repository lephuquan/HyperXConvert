package com.hyperxconvert.api.service;

import com.hyperxconvert.api.converter.FileConverterFactory;
import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.queue.FileMessage;
import com.hyperxconvert.api.queue.RabbitMQSender;
import com.hyperxconvert.api.repository.ConversionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ConversionRepository conversionRepository;
    private final S3StorageService s3StorageService;
    private final RabbitMQSender rabbitMQSender;
    private final FileConverterFactory fileConverterFactory;
    private final CreditService creditService;

    @Value("${conversion.expiry.days.basic:7}")
    private int basicExpiryDays;

    @Value("${conversion.expiry.days.premium:30}")
    private int premiumExpiryDays;

    /**
     * Create a new conversion
     *
     * @param user The user who is creating the conversion
     * @param file The file to convert
     * @param targetFormat The target format
     * @return The created conversion
     */
    @Transactional
    public Conversion createConversion(User user, MultipartFile file, String targetFormat) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename cannot be null");
        }

        // Get file extension
        String originalFormat = getFileExtension(originalFilename);
        
        // Check if conversion is supported
        if (!fileConverterFactory.isConversionSupported(originalFormat, targetFormat)) {
            throw new IllegalArgumentException(
                    "Conversion from " + originalFormat + " to " + targetFormat + " is not supported");
        }
        
        // Check if user has enough credits
//        int requiredCredits = calculateRequiredCredits(file.getSize(), originalFormat, targetFormat);
//        creditService.useCredits(user, requiredCredits);
        
        // Upload file to S3
        String s3Path = s3StorageService.uploadFile(file);
        
        // Create conversion entity
        Conversion conversion = Conversion.builder()
                .user(user)
                .originalFileName(originalFilename)
                .originalFileSize(file.getSize())
                .originalFormat(originalFormat)
                .targetFormat(targetFormat)
                .s3OriginalPath(s3Path)
                .status(Conversion.ConversionStatus.PENDING)
//                .creditsUsed(requiredCredits)
                .createdAt(LocalDateTime.now())
                .build();
        
        conversion = conversionRepository.save(conversion);
        
        // Send message to queue
        FileMessage message = new FileMessage(
                conversion.getId().toString(),
                s3Path,
                originalFormat,
                targetFormat);
        
        rabbitMQSender.sendMessage(message);
        
        return conversion;
    }

    /**
     * Get a conversion by ID
     *
     * @param id The conversion ID
     * @return The conversion
     */
    public Optional<Conversion> getConversion(Long id) {
        return conversionRepository.findById(id);
    }

    /**
     * Get conversions for a user
     *
     * @param user The user
     * @param pageable The pagination information
     * @return The conversions
     */
    public Page<Conversion> getConversionsForUser(User user, Pageable pageable) {
        return conversionRepository.findByUser(user, pageable);
    }

    /**
     * Update conversion status
     *
     * @param id The conversion ID
     * @param status The new status
     * @param s3ConvertedPath The S3 path of the converted file (if completed)
     * @param errorMessage The error message (if failed)
     * @return The updated conversion
     */
    @Transactional
    public Conversion updateConversionStatus(Long id, Conversion.ConversionStatus status, 
                                           String s3ConvertedPath, String errorMessage) {
        Conversion conversion = conversionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversion not found: " + id));
        
        conversion.setStatus(status);
        
        if (s3ConvertedPath != null) {
            conversion.setS3ConvertedPath(s3ConvertedPath);
            
            // Set expiry date based on user's subscription
            int expiryDays = basicExpiryDays;
            if (conversion.getUser().getSubscriptions() != null && !conversion.getUser().getSubscriptions().isEmpty()) {
                // Find active subscription
                boolean hasPremium = conversion.getUser().getSubscriptions().stream()
                        .anyMatch(s -> s.isActive() && 
                                (s.getPlanType() == Subscription.PlanType.PRO || 
                                 s.getPlanType() == Subscription.PlanType.ULTIMATE));
                
                if (hasPremium) {
                    expiryDays = premiumExpiryDays;
                }
            }
            
            conversion.setExpiryDate(LocalDateTime.now().plusDays(expiryDays));
        }
        
        if (errorMessage != null) {
            conversion.setErrorMessage(errorMessage);
        }
        
        return conversionRepository.save(conversion);
    }

    /**
     * Delete a conversion
     *
     * @param conversion The conversion to delete
     */
    @Transactional
    public void deleteConversion(Conversion conversion) {
        // Delete files from S3
        if (conversion.getS3OriginalPath() != null) {
            s3StorageService.deleteFile(conversion.getS3OriginalPath());
        }
        
        if (conversion.getS3ConvertedPath() != null) {
            s3StorageService.deleteFile(conversion.getS3ConvertedPath());
        }
        
        conversionRepository.delete(conversion);
    }

    /**
     * Delete a conversion by ID
     *
     * @param id The conversion ID
     */
    @Transactional
    public void deleteConversion(Long id) {
        Conversion conversion = conversionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversion not found: " + id));
        
        deleteConversion(conversion);
    }

    /**
     * Get file extension from filename
     *
     * @param filename The filename
     * @return The file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Calculate required credits for conversion
     *
     * @param fileSize The file size in bytes
     * @param sourceFormat The source format
     * @param targetFormat The target format
     * @return The required credits
     */
    private int calculateRequiredCredits(long fileSize, String sourceFormat, String targetFormat) {
        // Base credit cost: 1 credit per MB
        int baseCost = (int) Math.ceil(fileSize / (1024.0 * 1024.0));
        
        // Minimum cost is 1 credit
        baseCost = Math.max(1, baseCost);
        
        // Apply format-specific multipliers
        double formatMultiplier = 1.0;
        
        // Example: PDF to DOCX is more expensive
        if ("pdf".equalsIgnoreCase(sourceFormat) && "docx".equalsIgnoreCase(targetFormat)) {
            formatMultiplier = 1.5;
        }
        
        return (int) Math.ceil(baseCost * formatMultiplier);
    }
    
    /**
     * Get expired conversions
     *
     * @return The expired conversions
     */
    public List<Conversion> getExpiredConversions() {
        return conversionRepository.findByExpiryDateBeforeAndStatus(
                LocalDateTime.now(), Conversion.ConversionStatus.COMPLETED);
    }
    
    /**
     * Get the converted file as a Resource
     *
     * @param conversion The conversion
     * @return The converted file as a Resource
     */
    public Resource getConvertedFile(Conversion conversion) {
        if (conversion.getStatus() != Conversion.ConversionStatus.COMPLETED) {
            throw new IllegalStateException("Conversion is not completed yet");
        }
        
        if (conversion.getS3ConvertedPath() == null) {
            throw new IllegalStateException("Converted file path is not available");
        }
        
        // Download the file from S3
        File tempFile = s3StorageService.downloadToTemp(conversion.getS3ConvertedPath());
        
        // Return the file as a Resource
        return new FileSystemResource(tempFile);
    }
}
