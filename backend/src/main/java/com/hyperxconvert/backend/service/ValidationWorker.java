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
import com.hyperxconvert.backend.enums.FileStatus;
import java.io.IOException;
import com.hyperxconvert.backend.constant.FileFormatConstants;

/**
 * FileValidationWorker Service
 * Artifact ID: 550e8400-e29b-41d4-a716-446655440000
 */
@Slf4j
@Service
public class ValidationWorker {
    private final SqsClient sqsClient;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    private final Tika tika = new Tika();
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // 10 thread song song

    @Value("${aws.sqs.upload-queue}")
    private String uploadQueueUrl;
    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public ValidationWorker(SqsClient sqsClient, S3Client s3Client, ObjectMapper objectMapper,
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
    @Scheduled(fixedRate = 1000) // Hàm sẽ chạy mỗi 1 giây (1000ms), bất kể lần trước chạy xong hay chưa
    public void pollUploadQueue() { // Trigger
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
            log.error("Error polling upload queue", e);
        }
    }

    private void processMessage(Message message) {

        long start = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        UploadMessage uploadMessage = parseUploadMessage(message.body());
        if (uploadMessage == null) return;

        if (!validateUploadMessage(uploadMessage)) return;
        if (!validateS3Bucket()) return;

        try (InputStream s3InputStream = downloadFileFromS3(uploadMessage.getFilePath())) {
            if (!validateFileSize(s3InputStream, uploadMessage, now)) return;
            if (!validateMimeType(s3InputStream, uploadMessage, now)) return;
            if (!scanForVirus(uploadMessage.getFilePath(), uploadMessage, now)) return;

            updateFileStatus(uploadMessage.getFileId(), FileStatus.QUEUED_AND_VALIDATED.name());
            logConvertQueue(uploadMessage, FileStatus.QUEUED_AND_VALIDATED.name(), null, now, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error processing validation message - fileId: {}", 
                uploadMessage != null ? uploadMessage.getFileId() : "unknown", e);
            handleValidationFailure(uploadMessage, "PROCESSING_ERROR", now);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            deleteSqsMessage(message);
            log.info("File validation completed - fileId: {}, duration: {:.1f}s", 
                uploadMessage != null ? uploadMessage.getFileId() : "unknown", durationMs / 1000.0);
        }
    }

    private UploadMessage parseUploadMessage(String body) {
        try {
            // Try S3EventMessage
            S3EventMessage s3EventMessage = objectMapper.readValue(body, S3EventMessage.class);
            if (s3EventMessage.getRecords() != null && !s3EventMessage.getRecords().isEmpty()) {
                S3EventMessage.S3Record record = s3EventMessage.getRecords().get(0);
                if (record.getS3() != null && record.getS3().getObject() != null) {
                    String encodedKey = record.getS3().getObject().getKey();
                    String filePath = URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
                    String[] pathParts = filePath.split("/");
                    if (pathParts.length >= 3) {
                        String userIdOrIp = pathParts[1];
                        String fileName = pathParts[2];
                        int lastDotIndex = fileName.lastIndexOf('.');
                        if (lastDotIndex > 0) {
                            String fileIdStr = fileName.substring(0, lastDotIndex);
                            try {
                                UUID fileId = UUID.fromString(fileIdStr);
                                return new UploadMessage(fileId, userIdOrIp, filePath);
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid fileId format in S3 path: {}", fileIdStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Try UploadMessage
            try {
                return objectMapper.readValue(body, UploadMessage.class);
            } catch (Exception uploadMessageException) {
                log.warn("Failed to parse message as both S3EventMessage and UploadMessage - body: {}", body);
                log.debug("S3EventMessage parse error: {}", e.getMessage());
                log.debug("UploadMessage parse error: {}", uploadMessageException.getMessage());
            }
        }
        return null;
    }

    private boolean validateUploadMessage(UploadMessage msg) {
        if (msg.getFilePath() == null || msg.getFilePath().isEmpty()) {
            log.warn("Invalid upload message - filePath is null or empty");
            return false;
        }
        if (msg.getFileId() == null) {
            log.warn("Invalid upload message - fileId is null");
            return false;
        }
        if (msg.getUserIdOrIp() == null || msg.getUserIdOrIp().isEmpty()) {
            log.warn("Invalid upload message - userIdOrIp is null or empty");
            return false;
        }
        return true;
    }

    private boolean validateS3Bucket() {
        if (s3Bucket == null || s3Bucket.isEmpty()) {
            log.error("S3 bucket configuration is missing");
            return false;
        }
        return true;
    }

    private InputStream downloadFileFromS3(String filePath) throws Exception {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Bucket)
            .key(filePath)
            .build();
        return s3Client.getObject(getObjectRequest);
    }

    private boolean validateFileSize(InputStream s3InputStream, UploadMessage msg, LocalDateTime now) throws IOException {
        if (s3InputStream.available() > MAX_FILE_SIZE) {
            handleValidationFailure(msg, "FILE_TOO_LARGE", now);
            return false;
        }
        return true;
    }

    private boolean validateMimeType(InputStream s3InputStream, UploadMessage msg, LocalDateTime now) throws IOException {
        String mimeType = tika.detect(s3InputStream, msg.getFilePath());
        if (!FileFormatConstants.ALLOWED_CONTENT_TYPES.contains(mimeType)) {
            handleValidationFailure(msg, "UNSUPPORTED_FORMAT", now);
            return false;
        }
        return true;
    }

    private boolean scanForVirus(String filePath, UploadMessage msg, LocalDateTime now) {
        if (!simulateClamAVScan(filePath)) {
            handleValidationFailure(msg, "CORRUPTED_FILE", now);
            return false;
        }
        return true;
    }

    private void handleValidationFailure(UploadMessage uploadMessage, String errorCode, LocalDateTime startedAt) {
        updateFileStatus(uploadMessage.getFileId(), FileStatus.FAILED.name());
        logConvertQueue(uploadMessage, FileStatus.FAILED.name(), errorCode, startedAt, LocalDateTime.now());
    }

    private void updateFileStatus(UUID fileId, String status) {
        fileRepository.findById(fileId).ifPresent(file -> {
            file.setStatus(status);
            fileRepository.save(file);
        });
    }

    private void logConvertQueue(UploadMessage uploadMessage, String status, String errorCode, LocalDateTime createdAt, LocalDateTime endedAt) {
        ConvertLog logEntry = new ConvertLog(UUID.randomUUID(), uploadMessage.getFileId(), uploadMessage.getUserIdOrIp(), status, endedAt, errorCode, createdAt, endedAt);
        convertLogRepository.save(logEntry);
    }

    /**
     * Simulate ClamAV scan via Lambda. Returns true if file is clean, false if malware detected.
     */
    private boolean simulateClamAVScan(String filePath) {
        // TODO: Replace with actual Lambda call
        return true;
    }

    /**
     * Xóa message khỏi SQS queue
     */
    private void deleteSqsMessage(Message message) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(uploadQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
        } catch (Exception e) {
            log.warn("Failed to delete message from SQS", e);
        }
    }
} 