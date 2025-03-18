package com.hyperxconvert.api.queue;

import com.hyperxconvert.api.converter.FileConverterFactory;
import com.hyperxconvert.api.entity.Conversion;
import com.hyperxconvert.api.service.ConversionService;
import com.hyperxconvert.api.service.EmailService;
import com.hyperxconvert.api.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessor {

    private final ConversionService conversionService;
    private final S3StorageService s3StorageService;
    private final FileConverterFactory fileConverterFactory;
    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.name:file-conversion-queue}")
    public void processFileConversion(FileMessage message) {
        log.info("Received file conversion request: {}", message);
        
        try {
            // Update conversion status to PROCESSING
            Long conversionId = Long.parseLong(message.getConversionId());
            conversionService.updateConversionStatus(conversionId, Conversion.ConversionStatus.PROCESSING, null, null);
            
            // Download file from S3
            File sourceFile = s3StorageService.downloadToTemp(message.getS3Path());
            
            // Convert file
            File convertedFile = fileConverterFactory.convert(
                    sourceFile, 
                    message.getSourceFormat(), 
                    message.getTargetFormat());
            
            // Upload converted file to S3
            String s3Key = "converted/" + UUID.randomUUID() + "." + message.getTargetFormat();
            String s3ConvertedPath = s3StorageService.uploadFile(convertedFile, s3Key);
            
            // Update conversion status to COMPLETED
            Conversion conversion = conversionService.updateConversionStatus(
                    conversionId, 
                    Conversion.ConversionStatus.COMPLETED, 
                    s3ConvertedPath, 
                    null);
            
            // Clean up temporary files
            sourceFile.delete();
            convertedFile.delete();
            
            // Send email notification
            if (conversion.getUser().getEmail() != null) {
                String downloadUrl = "https://hyperxconvert.com/conversions/" + conversionId + "/download";
                emailService.sendConversionCompleteEmail(
                        conversion.getUser().getEmail(),
                        conversion.getUser().getFullName(),
                        conversion.getOriginalFileName(),
                        downloadUrl);
            }
            
            log.info("File conversion completed successfully: {}", conversionId);
        } catch (Exception e) {
            log.error("Error processing file conversion", e);
            
            try {
                // Update conversion status to FAILED
                Long conversionId = Long.parseLong(message.getConversionId());
                conversionService.updateConversionStatus(
                        conversionId, 
                        Conversion.ConversionStatus.FAILED, 
                        null, 
                        e.getMessage());
            } catch (Exception ex) {
                log.error("Error updating conversion status", ex);
            }
        }
    }
}
