package com.hyperxconvert.backend.service;

import com.hyperxconvert.backend.dto.ConvertQueueMessage;
import com.hyperxconvert.backend.dto.request.UploadUrlRequest;
import com.hyperxconvert.backend.dto.response.UploadUrlResponse;
import com.hyperxconvert.backend.dto.request.FileConvertRequest;
import com.hyperxconvert.backend.dto.response.FileConvertResponse;
import com.hyperxconvert.backend.entity.ConvertLog;
import com.hyperxconvert.backend.entity.File;
import com.hyperxconvert.backend.enums.FileStatus;
import com.hyperxconvert.backend.exception.ApiException;
import com.hyperxconvert.backend.repository.ConvertLogRepository;
import com.hyperxconvert.backend.repository.FileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import com.hyperxconvert.backend.constant.FileFormatConstants;
import com.hyperxconvert.backend.enums.FileFormat;
import com.hyperxconvert.backend.util.FilenameUtils;

@Slf4j
@Service
public class FileManagementService {
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    private final S3Service s3Service;
    private final int maxDailyUploads;
    private final Duration presignedUrlExpiry;
    private final MessageSource messageSource;

    @Autowired
    private SqsClient sqsClient;

    @Value("${aws.sqs.convert-queue}")
    private String convertQueueUrl;


        public FileManagementService(FileRepository fileRepository, ConvertLogRepository convertLogRepository, S3Service s3Service,
                                @Value("${app.upload.max-daily-uploads:5}") int maxDailyUploads,
                                @Value("${app.upload.presigned-url-expiry:PT24H}") String presignedUrlExpiry,
                                MessageSource messageSource) {
        this.fileRepository = fileRepository;
        this.convertLogRepository = convertLogRepository;
        this.s3Service = s3Service;
        this.maxDailyUploads = maxDailyUploads;
        this.presignedUrlExpiry = Duration.parse(presignedUrlExpiry);
        this.messageSource = messageSource;
    }

    @Transactional
    public UploadUrlResponse createPresignedUploadUrl(UploadUrlRequest request, HttpServletRequest httpRequest) {
        long startTime = System.nanoTime();
        try {
            String ipAddress = extractClientIp(httpRequest);
            validateLimit(ipAddress);
            validateFile(request);
            String extension = getExtension(request.getFileName());
            validateExtensionAndContentType(extension, request.getContentType());
            UUID fileId = UUID.randomUUID();
            String s3Key = String.format("uploads/%s/%s.%s", ipAddress, fileId, extension);
            String presignedUrl = s3Service.generatePresignedUploadUrl(s3Key, request.getContentType(), presignedUrlExpiry);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime expiresAt = now.plus(presignedUrlExpiry);
            saveFileAndLog(fileId, ipAddress, s3Key, extension, now, expiresAt, FilenameUtils.sanitizeFilename(request.getFileName()));
            return new UploadUrlResponse(fileId.toString(), presignedUrl, "URL_GENERATED", expiresAt.toString());
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("Upload URL generation completed - duration: {:.1f}s", durationMs / 1000.0);
        }
    }

    private void saveFileAndLog(UUID fileId, String ipAddress, String s3Key, String extension, LocalDateTime now, LocalDateTime expiresAt, String originalFilename) {
        File file = new File(fileId, ipAddress, s3Key, extension.toUpperCase(), null, FileStatus.UPLOADED.name(), expiresAt, originalFilename);
        file.setCreatedAt(now);
        file.setUpdatedAt(now);
        fileRepository.save(file);
        ConvertLog log = new ConvertLog();
        log.setId(UUID.randomUUID());
        log.setFileId(fileId);
        log.setUserIp(ipAddress);
        log.setStatus(FileStatus.UPLOADED.name());
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        convertLogRepository.save(log);
    }

    public FileConvertResponse processFileConvertRequest(FileConvertRequest request) {
        UUID fileId = parseFileId(request.getFileId());
        File file = getFileOrThrow(fileId);
        validateFileStatusForConversion(file);
        String targetFormat = validateAndGetTargetFormat(request.getTargetFormat());
        UUID jobId = logConversionJob(fileId, file.getUserIp());
        sendMessageToSqsWithRetry(fileId, targetFormat, file.getOriginalPath());
        return new FileConvertResponse(jobId.toString(), FileStatus.QUEUED_AND_VALIDATED.name());
    }

    private UUID parseFileId(String fileIdStr) {
        try {
            return UUID.fromString(fileIdStr);
        } catch (Exception e) {
            throw new ApiException("FILE_NOT_FOUND", "error.file.not.found");
        }
    }

    private File getFileOrThrow(UUID fileId) {
        return fileRepository.findById(fileId).orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "error.file.not.found"));
    }

    private void validateFileStatusForConversion(File file) {
        if (!FileStatus.QUEUED_AND_VALIDATED.name().equalsIgnoreCase(file.getStatus())) {
            log.warn("File not ready for conversion - fileId: {}, status: {}", file.getFileId(), file.getStatus());
            throw new ApiException("FILE_NOT_READY", "error.file.not.ready");
        }
    }

    private String validateAndGetTargetFormat(String targetFormat) {
        FileFormat format = FileFormat.fromString(targetFormat);
        if (format == null || !FileFormatConstants.SUPPORTED_FORMATS.contains(format)) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
        return format.name();
    }

    private UUID logConversionJob(UUID fileId, String userIp) {
        UUID jobId = UUID.randomUUID();
        ConvertLog log = new ConvertLog();
        log.setId(jobId);
        log.setFileId(fileId);
        log.setUserIp(userIp);
        log.setStatus(FileStatus.QUEUED_AND_CONVERTED.name());
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        convertLogRepository.save(log);
        return jobId;
    }

    private void sendMessageToSqsWithRetry(UUID fileId, String targetFormat, String originalPath) {
        int maxRetries = 3;
        int[] backoffSeconds = {2, 4, 8};
        boolean sent = false;
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String messageBody = objectMapper.writeValueAsString(
                    new ConvertQueueMessage(fileId.toString(), targetFormat, originalPath)
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
            log.error("Failed to send SQS message after {} retries", maxRetries, lastException);
            throw new ApiException("SYSTEM_ERROR", "error.internal");
        }
    }

    public Map<String, Object> getFileStatus(String fileId) {
        try {
            UUID uuid = UUID.fromString(fileId);
            File file = fileRepository.findById(uuid).orElseThrow(() -> new ApiException("FILE_NOT_FOUND", "error.file.not.found"));
            Map<String, Object> result = new HashMap<>();
            result.put("status", file.getStatus());
            if (FileStatus.SUCCESS.name().equalsIgnoreCase(file.getStatus()) && file.getConvertedPath() != null) {
                // Generate filename for download with original name + converted extension
                String originalFilename = file.getOriginalFilename();
                FileFormat targetFormat = FileFormat.fromString(file.getFormatTo());
                String extension = targetFormat != null ? targetFormat.getExtension() : file.getFormatTo().toLowerCase();
                String downloadFilename = FilenameUtils.getFilenameWithExtension(originalFilename, extension);
                
                String presignedUrl = s3Service.generatePresignedDownloadUrl(file.getConvertedPath(), downloadFilename, presignedUrlExpiry);
                result.put("downloadUrl", presignedUrl);
            }
            return result;
        } catch (ApiException e) {
            log.warn("File not found - fileId: {}", fileId);
            throw e;
        } catch (Exception e) {
            log.error("Internal error in getFileStatus - fileId: {}", fileId, e);
            throw new RuntimeException("SYSTEM_ERROR");
        }
    }

    public Map<String, Object> getDownloadUrl(String fileId) {
        try {
            UUID uuid = UUID.fromString(fileId);
            File file = fileRepository.findById(uuid).orElseThrow(() -> new ApiException("FILE_NOT_READY", "File does not exist or has not been successfully converted"));
            if (!FileStatus.SUCCESS.name().equalsIgnoreCase(file.getStatus()) || file.getConvertedPath() == null) {
                log.warn("File not ready for download - fileId: {}, status: {}, convertedPath: {}", 
                    fileId, file.getStatus(), file.getConvertedPath());
                throw new ApiException("FILE_NOT_READY", "File does not exist or has not been successfully converted");
            }
            
            // Generate filename for download with original name + converted extension
            String originalFilename = file.getOriginalFilename();
            FileFormat targetFormat = FileFormat.fromString(file.getFormatTo());
            String extension = targetFormat != null ? targetFormat.getExtension() : file.getFormatTo().toLowerCase();
            String downloadFilename = FilenameUtils.getFilenameWithExtension(originalFilename, extension);
            
            String presignedUrl = s3Service.generatePresignedDownloadUrl(file.getConvertedPath(), downloadFilename, presignedUrlExpiry);
            Map<String, Object> result = new HashMap<>();
            result.put("preSignedUrl", presignedUrl);
            return result;
        } catch (ApiException e) {
            log.warn("File not ready for download - fileId: {}", fileId);
            throw e;
        } catch (Exception e) {
            log.error("System error when generating download URL - fileId: {}", fileId, e);
            throw new RuntimeException("SYSTEM_ERROR");
        }
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
        if (!FileFormatConstants.ALLOWED_CONTENT_TYPES.contains(request.getContentType())) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
    }

    private void validateExtensionAndContentType(String extension, String contentType) {
        if (!FileFormatConstants.ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ApiException("UNSUPPORTED_FORMAT", "error.unsupported.format");
        }
        if (!isExtensionMatchContentType(extension, contentType)) {
            throw new ApiException("INVALID_EXTENSION", "error.invalid.extension");
        }
    }

    private boolean isExtensionMatchContentType(String extension, String contentType) {
        String expectedContentType = FileFormatConstants.EXTENSION_TO_CONTENT_TYPE.get(extension.toLowerCase());
        return contentType.equals(expectedContentType);
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

} 