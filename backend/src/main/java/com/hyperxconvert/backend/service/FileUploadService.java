package com.hyperxconvert.backend.service;

import com.hyperxconvert.backend.dto.UploadUrlRequest;
import com.hyperxconvert.backend.dto.UploadUrlResponse;
import com.hyperxconvert.backend.entity.File;
import com.hyperxconvert.backend.enums.FileStatus;
import com.hyperxconvert.backend.repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.net.URI;

@Service
public class FileUploadService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${app.upload.max-daily-uploads:5}")
    private int maxDailyUploads;
    
    @Value("${app.upload.presigned-url-expiry:PT24H}")
    private String presignedUrlExpiry;
    
    @Value("${aws.endpoint:}")
    private String awsEndpoint;
    
    public FileUploadService(S3Client s3Client, FileRepository fileRepository) {
        this.s3Client = s3Client;
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(s3Client.serviceClientConfiguration().region())
                .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider());
        if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(awsEndpoint));
        }
        this.s3Presigner = presignerBuilder.build();
        this.fileRepository = fileRepository;
    }
    
    /**
     * Generate a presigned URL for file upload
     */
    public UploadUrlResponse generateUploadUrl(UploadUrlRequest request, String userIp) {
        // Validate daily upload limit
        validateDailyUploadLimit(userIp);
        
        // Generate file ID and key
        UUID fileId = UUID.randomUUID();
        String fileKey = generateFileKey(fileId, request.getFilename(), userIp);
        
        // Generate presigned URL
        String presignedUrl = generatePresignedUrl(fileKey, request.getFilename());
        
        // Save file metadata to database
        saveFileMetadata(fileId, userIp, fileKey, request.getFilename());
        
        return new UploadUrlResponse(presignedUrl, fileKey, fileId);
    }
    
    /**
     * Validate daily upload limit for the given IP address
     */
    private void validateDailyUploadLimit(String userIp) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long uploadCount = fileRepository.countByUserIpAndUploadTimeAfter(userIp, startOfDay);
        
        if (uploadCount >= maxDailyUploads) {
            throw new RuntimeException("Daily upload limit exceeded (5 files per day per IP)");
        }
    }
    
    /**
     * Generate S3 file key
     */
    private String generateFileKey(UUID fileId, String filename, String userIp) {
        String extension = getFileExtension(filename);
        return String.format("uploads/%s/%s%s", userIp, fileId, extension);
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    /**
     * Generate presigned URL for S3 upload
     */
    private String generatePresignedUrl(String fileKey, String filename) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(getContentType(filename))
                .build();
        
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.parse(presignedUrlExpiry))
                .putObjectRequest(putObjectRequest)
                .build();
        
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }
    
    /**
     * Get content type based on file extension
     */
    private String getContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        switch (extension) {
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt":
                return "application/vnd.ms-powerpoint";
            case ".pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".mp4":
                return "video/mp4";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * Save file metadata to database
     */
    private void saveFileMetadata(UUID fileId, String userIp, String fileKey, String filename) {
        String formatFrom = getFileExtension(filename).substring(1).toUpperCase();
        LocalDateTime uploadTime = LocalDateTime.now();
        LocalDateTime expiryTime = uploadTime.plusHours(24);
        
        File file = new File(fileId, userIp, fileKey, formatFrom, "PDF", FileStatus.PENDING.getValue(), uploadTime, expiryTime);
        fileRepository.save(file);
    }
    
    /**
     * Get user IP address from request
     */
    public String getUserIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 