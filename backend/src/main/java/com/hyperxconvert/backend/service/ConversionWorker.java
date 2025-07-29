package com.hyperxconvert.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperxconvert.backend.dto.ConvertQueueMessage;
import com.hyperxconvert.backend.entity.ConvertLog;
import com.hyperxconvert.backend.repository.FileRepository;
import com.hyperxconvert.backend.repository.ConvertLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import com.hyperxconvert.backend.enums.FileStatus;
import org.apache.tika.Tika;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileInputStream;
import com.hyperxconvert.backend.constant.FileFormatConstants;
import com.hyperxconvert.backend.enums.FileFormat;
import org.springframework.context.MessageSource;
import java.util.Locale;
import com.hyperxconvert.backend.util.FilenameUtils;

@Slf4j
@Service
public class ConversionWorker {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    private final MessageSource messageSource;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ConversionGateway fileConversionService;

    private final Tika tika = new Tika();
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_SECONDS = {2, 4, 8};

    @Value("${aws.sqs.convert-queue}")
    private String convertQueueUrl;

    public ConversionWorker(SqsClient sqsClient, ObjectMapper objectMapper,
                            FileRepository fileRepository, ConvertLogRepository convertLogRepository,
                            MessageSource messageSource) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.fileRepository = fileRepository;
        this.convertLogRepository = convertLogRepository;
        this.messageSource = messageSource;
    }

    @Scheduled(fixedRate = 1000)
    public void pollConvertQueue() {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(convertQueueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(10)
                .build();
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
        for (Message msg : messages) {
            processMessage(msg);
        }
    }

    private boolean isDocxFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[2];
            if (fis.read(header) != 2) return false;
            return header[0] == 'P' && header[1] == 'K';
        } catch (Exception e) {
            return false;
        }
    }

    private void processMessage(Message msg) {
        long startTime = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        com.hyperxconvert.backend.entity.File fileEntity = null;
        File inputFile = null;
        File convertedFile = null;
        String errorCode = null;
        int receiveCount = 1;
        ConvertQueueMessage job = null;
        try {
            if (msg.attributes().containsKey("ApproximateReceiveCount")) {
                receiveCount = Integer.parseInt(msg.attributes().get("ApproximateReceiveCount"));
            }
            job = objectMapper.readValue(msg.body(), ConvertQueueMessage.class);
            log.info("Processing conversion job - fileId: {}, targetFormat: {}, attempt: {}", 
                job.getFileId(), job.getTargetFormat(), receiveCount);
            // 1. Get file entity from DB
            Optional<com.hyperxconvert.backend.entity.File> fileOpt = fileRepository.findById(UUID.fromString(job.getFileId()));
            if (fileOpt.isEmpty()) throw new Exception("error.file.not.found");
            fileEntity = fileOpt.get();
            // 2. Download file from S3
            inputFile = s3Service.downloadFileFromS3(job.getOriginalPath());
            // 2.1 If converting DOCX -> PDF, copy temp file as .docx
            File realInputFile = inputFile;
            FileFormat formatFromEnum = FileFormat.fromString(fileEntity.getFormatFrom());
            FileFormat targetFormatEnum = FileFormat.fromString(job.getTargetFormat());
            if (formatFromEnum == FileFormat.DOCX && targetFormatEnum == FileFormat.PDF) {
                File docxFile = Files.createTempFile("input-", ".docx").toFile();
                Files.copy(inputFile.toPath(), docxFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                realInputFile = docxFile;
                // Validate input file is actually DOCX
                if (!isDocxFile(realInputFile)) {
                    throw new Exception("error.invalid.extension");
                }
            }
            // 3. Validate file size
            if (realInputFile.length() > MAX_FILE_SIZE) throw new Exception("error.file.too.large");
            // 3. Validate format
            String mimeType = tika.detect(realInputFile);
            FileFormat detectedFormat = FileFormatConstants.MIME_TYPE_TO_FORMAT.get(mimeType);
            boolean supported = FileFormatConstants.SUPPORTED_FORMATS.contains(detectedFormat);
            if (!supported) throw new Exception("error.unsupported.format");
            // 4. Virus scan (skipped)
            // 5. Convert file
            convertedFile = fileConversionService.convert(realInputFile, fileEntity.getFormatFrom(), job.getTargetFormat());
            // 6. Upload result file to S3 with original filename
            FileFormat targetFormat = FileFormat.fromString(job.getTargetFormat());
            String fileExtension = targetFormat != null ? targetFormat.getExtension() : job.getTargetFormat().toLowerCase();
            
            // Generate filename with original name + converted extension
            String originalFilename = fileEntity.getOriginalFilename();
            String convertedFilename = FilenameUtils.getFilenameWithExtension(originalFilename, fileExtension);
            
            String convertedKey = String.format("converted/%s/%s", fileEntity.getUserIp(), convertedFilename);
            s3Service.uploadFileToS3(convertedKey, convertedFile, "application/octet-stream");
            // 7. Update DB: files, convert_queue_logs
            fileEntity.setStatus(FileStatus.SUCCESS.name());
            fileEntity.setConvertedPath(convertedKey);
            fileEntity.setFormatTo(job.getTargetFormat());
            fileRepository.save(fileEntity);
            ConvertLog logEntry = new ConvertLog(UUID.randomUUID(), fileEntity.getFileId(), fileEntity.getUserIp(), FileStatus.SUCCESS.name(), now, null, now, now);
            convertLogRepository.save(logEntry);
            // 8. Delete message from queue
            sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(convertQueueUrl).receiptHandle(msg.receiptHandle()).build());
        } catch (Exception e) {
            errorCode = e.getMessage();
            String errorMsg = errorCode;
            try {
                errorMsg = messageSource.getMessage(errorCode, null, errorCode, Locale.getDefault());
            } catch (Exception ex) {
                // fallback to errorCode
            }
            log.error("Conversion job failed - fileId: {}, error: {}", 
                job != null ? job.getFileId() : "unknown", errorMsg, e);
            // Retry if not exceeded max attempts
            if (msg.attributes().containsKey("ApproximateReceiveCount")) {
                receiveCount = Integer.parseInt(msg.attributes().get("ApproximateReceiveCount"));
            }
            if (receiveCount < MAX_RETRIES) {
                int backoff = BACKOFF_SECONDS[Math.min(receiveCount - 1, BACKOFF_SECONDS.length - 1)];
                try {
                    Thread.sleep(backoff * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Will retry conversion job after {} seconds - fileId: {}, attempt: {}", 
                    backoff, job != null ? job.getFileId() : "unknown", receiveCount + 1);
                // Do not delete message, SQS will retry
            } else {
                // Send to DLQ (or let SQS handle if RedrivePolicy is set)
                log.error("Exceeded max retry attempts for conversion job - fileId: {}, sending to DLQ", 
                    job != null ? job.getFileId() : "unknown");
                // Delete message from main queue to avoid infinite loop
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(convertQueueUrl).receiptHandle(msg.receiptHandle()).build());
            }
            // Update DB status to FAILED
            if (fileEntity != null) {
                fileEntity.setStatus(FileStatus.FAILED.name());
                fileRepository.save(fileEntity);
                String shortErrorCode = (errorCode != null && errorCode.length() > 50) ? errorCode.substring(0, 50) : errorCode;
                ConvertLog logEntry = new ConvertLog(UUID.randomUUID(), fileEntity.getFileId(), fileEntity.getUserIp(), FileStatus.FAILED.name(), now, shortErrorCode, now, now);
                convertLogRepository.save(logEntry);
            }
        } finally {
            // Clean up temp files
            if (inputFile != null && inputFile.exists()) inputFile.delete();
            if (convertedFile != null && convertedFile.exists()) convertedFile.delete();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("Conversion job completed - fileId: {}, duration: {:.1f}s", 
                job != null ? job.getFileId() : "unknown", durationMs / 1000.0);
        }
    }

} 