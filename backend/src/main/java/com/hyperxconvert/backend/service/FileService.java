package com.hyperxconvert.backend.service;

import com.hyperxconvert.backend.dto.UploadUrlRequest;
import com.hyperxconvert.backend.dto.UploadUrlResponse;
import com.hyperxconvert.backend.dto.FileConvertRequest;
import com.hyperxconvert.backend.dto.FileConvertResponse;
import com.hyperxconvert.backend.entity.ConvertLog;
import com.hyperxconvert.backend.entity.File;
import com.hyperxconvert.backend.enums.FileStatus;
import com.hyperxconvert.backend.exception.ApiException;
import com.hyperxconvert.backend.repository.ConvertLogRepository;
import com.hyperxconvert.backend.repository.FileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class FileService {
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    private final S3Service s3Service;
    private final int maxDailyUploads;
    private final MessageSource messageSource;
    private final List<String> allowedContentTypes = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png",
            "video/mp4"
    );
    private final List<String> allowedExtensions = Arrays.asList("pdf", "docx", "jpg", "png", "mp4");

    @Autowired
    private SqsClient sqsClient;

    @Value("${aws.sqs.convert-queue}")
    private String convertQueueUrl;

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "PDF", "DOCX", "JPG", "PNG", "MP3", "COMPRESSED_PDF", "COMPRESSED_VIDEO"
    );

    public FileService(FileRepository fileRepository, ConvertLogRepository convertLogRepository, S3Service s3Service,
                      @Value("${app.upload.max-daily-uploads:5}") int maxDailyUploads,
                      MessageSource messageSource) {
        this.fileRepository = fileRepository;
        this.convertLogRepository = convertLogRepository;
        this.s3Service = s3Service;
        this.maxDailyUploads = maxDailyUploads;
        this.messageSource = messageSource;
    }

    @Transactional
    public UploadUrlResponse createPresignedUploadUrl(UploadUrlRequest request, HttpServletRequest httpRequest) {
        String ipAddress = extractClientIp(httpRequest);
        validateLimit(ipAddress);
        validateFile(request);
        String extension = getExtension(request.getFileName());
        validateExtensionAndContentType(extension, request.getContentType());
        UUID fileId = UUID.randomUUID();
        String s3Key = String.format("uploads/%s/%s.%s", ipAddress, fileId, extension);
        String presignedUrl = s3Service.generatePresignedUploadUrl(s3Key, request.getContentType());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plusHours(24);
        // Lưu file metadata
        File file = new File(fileId, ipAddress, s3Key, extension.toUpperCase(), null, FileStatus.UPLOADED.name(), now, expiresAt);
        fileRepository.save(file);
        // Lưu log convert
        ConvertLog log = new ConvertLog();
        log.setId(UUID.randomUUID());
        log.setFileId(fileId);
        log.setUserIp(ipAddress);
        log.setStatus("UPLOADED");
        log.setStartedAt(now);
        log.setCreatedAt(now);
        convertLogRepository.save(log);
        return new UploadUrlResponse(fileId.toString(), presignedUrl, "URL_GENERATED", expiresAt.toString());
    }

    public FileConvertResponse processFileConvertRequest(FileConvertRequest request) {
        // 1. Kiểm tra fileId tồn tại
        UUID fileId;
        try {
            fileId = UUID.fromString(request.getFileId());
        } catch (Exception e) {
            throw new ApiException("FILE_NOT_FOUND", "error.file.not.found");
        }
        Optional<File> fileOpt = fileRepository.findById(fileId);
        if (fileOpt.isEmpty()) {
            throw new ApiException("FILE_NOT_FOUND", "error.file.not.found");
        }
        File file = fileOpt.get();
        // 2. Kiểm tra trạng thái file
        if (!"READY".equalsIgnoreCase(file.getStatus())) {
            throw new ApiException("FILE_NOT_READY", "error.file.not.ready");
        }
        // 3. Validate targetFormat
        String targetFormat = request.getTargetFormat().toUpperCase();
        if (!SUPPORTED_FORMATS.contains(targetFormat)) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
        // 4. Ghi log job
        UUID jobId = UUID.randomUUID();
        ConvertLog log = new ConvertLog();
        log.setId(jobId);
        log.setFileId(fileId);
        log.setUserIp(file.getUserIp());
        log.setStatus("QUEUED");
        log.setStartedAt(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        convertLogRepository.save(log);
        // 5. Đẩy message vào SQS với retry logic
        int maxRetries = 3;
        int[] backoffSeconds = {2, 4, 8};
        boolean sent = false;
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String messageBody = objectMapper.writeValueAsString(
                    new ConvertQueueMessage(fileId.toString(), targetFormat, file.getOriginalPath())
                );
                SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                        .queueUrl(convertQueueUrl)
                        .messageBody(messageBody)
                        .build();
                sqsClient.sendMessage(sendMsgRequest);
                sent = true;
                break;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(backoffSeconds[attempt] * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        if (!sent) {
            throw new ApiException("SYSTEM_ERROR", "error.internal");
        }
        return new FileConvertResponse(jobId.toString(), "PROCESSING");
    }

    private void validateLimit(String ipAddress) {
        LocalDateTime startOfDay = LocalDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long count = convertLogRepository.countByUserIpAndCreatedAtAfter(ipAddress, startOfDay);
        if (count >= maxDailyUploads) {
            throw new ApiException("LIMIT_EXCEEDED", "error.limit.exceeded");
        }
    }

    private void validateFile(UploadUrlRequest request) {
        if (request.getFileSize() > 52428800) {
            throw new ApiException("FILE_TOO_LARGE", "error.file.too.large");
        }
        if (!allowedContentTypes.contains(request.getContentType())) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
    }

    private void validateExtensionAndContentType(String extension, String contentType) {
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
        if (!isExtensionMatchContentType(extension, contentType)) {
            throw new ApiException("INVALID_EXTENSION", "error.invalid.extension");
        }
    }

    private boolean isExtensionMatchContentType(String extension, String contentType) {
        switch (extension.toLowerCase()) {
            case "pdf": return contentType.equals("application/pdf");
            case "docx": return contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "jpg": return contentType.equals("image/jpeg");
            case "png": return contentType.equals("image/png");
            case "mp4": return contentType.equals("video/mp4");
            default: return false;
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            throw new ApiException("INVALID_EXTENSION", "error.invalid.extension");
        }
        return fileName.substring(lastDot + 1);
    }

    public String extractClientIp(HttpServletRequest request) {
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

    // DTO cho message gửi vào convert-queue
    public static class ConvertQueueMessage {
        private String fileId;
        private String targetFormat;
        private String originalPath;

        public ConvertQueueMessage(String fileId, String targetFormat, String originalPath) {
            this.fileId = fileId;
            this.targetFormat = targetFormat;
            this.originalPath = originalPath;
        }

        public String getFileId() { return fileId; }
        public String getTargetFormat() { return targetFormat; }
        public String getOriginalPath() { return originalPath; }
    }
} 