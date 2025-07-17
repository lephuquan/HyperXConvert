package com.hyperxconvert.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperxconvert.backend.dto.UploadMessage;
import com.hyperxconvert.backend.dto.S3EventMessage;
import com.hyperxconvert.backend.entity.ConvertLog;
import com.hyperxconvert.backend.repository.FileRepository;
import com.hyperxconvert.backend.repository.ConvertLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FileValidationWorker Service
 * Artifact ID: 550e8400-e29b-41d4-a716-446655440000
 */
@Slf4j
@Service
public class FileValidationWorker {
    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    private final Tika tika = new Tika();
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // 5 thread song song

    @Value("${aws.sqs.upload-queue}")
    private String uploadQueueUrl;
//    @Value("${aws.sqs.convert-queue}")
//    private String convertQueueUrl;
    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final List<String> SUPPORTED_MIME_TYPES = List.of(
            "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png", "video/mp4"
    );

    public FileValidationWorker(SqsClient sqsClient, S3Client s3Client, ObjectMapper objectMapper,
                               FileRepository fileRepository, ConvertLogRepository convertLogRepository) {
        this.sqsClient = sqsClient;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.fileRepository = fileRepository;
        this.convertLogRepository = convertLogRepository;
    }

    /**
     * Polls the upload-queue every 5 seconds, processes up to 10 messages at a time.
     */
    @Scheduled(fixedRate = 1000)
    public void pollUploadQueue() {// Trigger
        log.info("Bắt đầu pollUploadQueue()");
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(uploadQueueUrl) // URL của hàng đợi SQS cần lấy tin nhắn
                    .maxNumberOfMessages(10) // Số lượng tin nhắn tối đa muốn nhận mỗi lần
                    .visibilityTimeout(90) // Thời gian chờ hiển thị tin nhắn (giây) - thời gian mà tin nhắn sẽ không được nhận bởi các worker khác
                    .waitTimeSeconds(20) // Thời gian chờ tối đa (giây) để nhận tin nhắn - giúp giảm số lượng yêu cầu đến SQS
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages(); // Nhận danh sách tin nhắn từ hàng đợi SQS
            for (Message message : messages) {
                executor.submit(() -> processMessage(message)); // Xử lý song song
            }
        } catch (Exception e) {
            log.error("[FileValidationWorker] Error polling SQS: {}", e.getMessage(), e);
        }
        log.info("Kết thúc pollUploadQueue()");
    }

    private void processMessage(Message message) {
        log.info("Bắt đâu validate file từ pool upload");
        long start = System.nanoTime();
        LocalDateTime startedAt = LocalDateTime.now();
        UploadMessage uploadMessage = null;
        String filePath = null;
        UUID fileId = null;
        String userIdOrIp = null;
        
        try {
            log.info("Processing message body: {}", message.body());
            
            // Try to parse as S3EventMessage first (new format)
            try {
                log.debug("Attempting to parse message as S3EventMessage...");
                S3EventMessage s3EventMessage = objectMapper.readValue(message.body(), S3EventMessage.class);
                log.debug("Successfully parsed as S3EventMessage");
                log.debug("Records size: {}", s3EventMessage.getRecords() != null ? s3EventMessage.getRecords().size() : "null");
                
                if (s3EventMessage.getRecords() != null && !s3EventMessage.getRecords().isEmpty()) {
                    S3EventMessage.S3Record record = s3EventMessage.getRecords().get(0);
                    log.debug("Processing S3 record: eventName={}, bucket={}", 
                             record.getEventName(), record.getS3() != null ? record.getS3().getBucket().getName() : "null");
                    
                    if (record.getS3() != null && record.getS3().getObject() != null) {
                        // Extract file path from S3 object key
                        String encodedKey = record.getS3().getObject().getKey();
                        filePath = URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
                        log.debug("Decoded file path: {} -> {}", encodedKey, filePath);
                        
                        // Extract fileId and userIdOrIp from the file path
                        // Expected format: uploads/{userIdOrIp}/{fileId}.{extension}
                        String[] pathParts = filePath.split("/");
                        log.debug("Path parts: {}", String.join(", ", pathParts));
                        
                        if (pathParts.length >= 3) {
                            userIdOrIp = pathParts[1];
                            String fileName = pathParts[2];
                            int lastDotIndex = fileName.lastIndexOf('.');
                            if (lastDotIndex > 0) {
                                String fileIdStr = fileName.substring(0, lastDotIndex);
                                try {
                                    fileId = UUID.fromString(fileIdStr);
                                    log.debug("Extracted fileId: {} from fileName: {}", fileId, fileName);
                                } catch (IllegalArgumentException e) {
                                    log.error("Invalid fileId format in path: {}", fileIdStr);
                                    return;
                                }
                            } else {
                                log.error("No file extension found in fileName: {}", fileName);
                                return;
                            }
                        } else {
                            log.error("Invalid path format. Expected: uploads/{userIdOrIp}/{fileId}.{extension}, Got: {}", filePath);
                            return;
                        }
                        
                        log.info("Successfully parsed S3EventMessage: fileId={}, userIdOrIp={}, filePath={}", 
                               fileId, userIdOrIp, filePath);
                    } else {
                        log.error("S3 object or bucket information is missing in the record");
                        return;
                    }
                } else {
                    log.error("No records found in S3EventMessage");
                    return;
                }
            } catch (Exception e) {
                log.debug("Failed to parse as S3EventMessage, trying UploadMessage format: {}", e.getMessage());
                
                // Try to parse as UploadMessage (legacy format)
                try {
                    uploadMessage = objectMapper.readValue(message.body(), UploadMessage.class);
                    fileId = uploadMessage.getFileId();
                    userIdOrIp = uploadMessage.getUserIdOrIp();
                    filePath = uploadMessage.getFilePath();
                    
                    log.info("Successfully parsed UploadMessage: fileId={}, userIdOrIp={}, filePath={}", 
                           fileId, userIdOrIp, filePath);
                } catch (Exception uploadMessageException) {
                    log.error("Failed to parse message as both S3EventMessage and UploadMessage. Message body: {}", message.body());
                    log.error("S3EventMessage parse error: {}", e.getMessage());
                    log.error("UploadMessage parse error: {}", uploadMessageException.getMessage());
                    return;
                }
            }
            
            // Validate required fields
            if (filePath == null || filePath.isEmpty()) {
                log.error("filePath is null or empty in message");
                return;
            }
            
            if (fileId == null) {
                log.error("fileId is null in message");
                return;
            }
            
            if (userIdOrIp == null || userIdOrIp.isEmpty()) {
                log.error("userIdOrIp is null or empty in message");
                return;
            }
            
            // Validate S3 bucket configuration
            if (s3Bucket == null || s3Bucket.isEmpty()) {
                log.error("S3 bucket is not configured");
                return;
            }
            
            log.info("Using S3 bucket: {}", s3Bucket);
            
            // Create UploadMessage object for consistent processing
            if (uploadMessage == null) {
                uploadMessage = new UploadMessage(fileId, userIdOrIp, filePath);
            }
           
            // Download file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(filePath)
                    .build();
            try (InputStream s3InputStream = s3Client.getObject(getObjectRequest)) {
                // Validate file size
                long fileSize = s3InputStream.available();
                if (fileSize > MAX_FILE_SIZE) {
                    handleValidationFailure(uploadMessage, "FILE_TOO_LARGE", startedAt);
//                    deleteMessage(message);
                    return;
                }
                // Validate MIME type
                String mimeType = tika.detect(s3InputStream, filePath);
                if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
                    handleValidationFailure(uploadMessage, "UNSUPPORTED_FORMAT", startedAt);
//                    deleteMessage(message);
                    return;
                }
                // Simulate ClamAV scan via Lambda
                boolean isClean = simulateClamAVScan(filePath);
                if (!isClean) {
                    handleValidationFailure(uploadMessage, "CORRUPTED_FILE", startedAt);
//                    deleteMessage(message);
                    return;
                }
                // If valid: send to convert-queue, update DB
//                sendToConvertQueue(message.body());
                updateFileStatus(fileId, "PROCESSING");
                logConvertQueue(uploadMessage, "PROCESSING", null, startedAt, LocalDateTime.now());
//                deleteMessage(message);
            }
        } catch (S3Exception s3e) {
            log.error("[FileValidationWorker] S3 error: {}", s3e.getMessage(), s3e);
            // Let SQS retry by not deleting the message
        } catch (Exception e) {
            log.error("[FileValidationWorker] Error processing message: {}", e.getMessage(), e);
            if (uploadMessage != null) {
                handleValidationFailure(uploadMessage, "PROCESSING_ERROR", startedAt);
            }
            // Let SQS retry by not deleting the message
        } finally {
            long end = System.nanoTime();
            long durationMs = (end - start) / 1_000_000;
            log.info("[FileValidationWorker] processMessage for message hash {} took {} ms", message.body().hashCode(), durationMs);
            log.info("Kết thúc validate file từ pool upload");
        }
    }

    private void handleValidationFailure(UploadMessage uploadMessage, String errorCode, LocalDateTime startedAt) {
        updateFileStatus(uploadMessage.getFileId(), "FAILED");
        logConvertQueue(uploadMessage, "FAILED", errorCode, startedAt, LocalDateTime.now());
    }

    private void updateFileStatus(UUID fileId, String status) {
        fileRepository.findById(fileId).ifPresent(file -> {
            file.setStatus(status);
            fileRepository.save(file);
        });
    }

    private void logConvertQueue(UploadMessage uploadMessage, String status, String errorCode, LocalDateTime startedAt, LocalDateTime endedAt) {
        ConvertLog logEntry = new ConvertLog();
        logEntry.setId(UUID.randomUUID());
        logEntry.setFileId(uploadMessage.getFileId());
        logEntry.setUserIp(uploadMessage.getUserIdOrIp());
        logEntry.setStatus(status);
        logEntry.setStartedAt(startedAt);
        logEntry.setEndedAt(endedAt);
        logEntry.setErrorCode(errorCode);
        logEntry.setCreatedAt(LocalDateTime.now());
        convertLogRepository.save(logEntry);
    }

//    private void sendToConvertQueue(String messageBody) {
//        SendMessageRequest sendRequest = SendMessageRequest.builder()
//                .queueUrl(convertQueueUrl)
//                .messageBody(messageBody)
//                .build();
//        sqsClient.sendMessage(sendRequest);
//    }

//    private void deleteMessage(Message message) {
//        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
//                .queueUrl(uploadQueueUrl)
//                .receiptHandle(message.receiptHandle())
//                .build();
//        sqsClient.deleteMessage(deleteRequest);
//    }

    /**
     * Simulate ClamAV scan via Lambda. Returns true if file is clean, false if malware detected.
     */
    private boolean simulateClamAVScan(String filePath) {
        // TODO: Replace with actual Lambda call
        return true;
    }
} 