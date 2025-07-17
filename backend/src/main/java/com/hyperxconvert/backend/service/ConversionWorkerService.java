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
import com.hyperxconvert.backend.service.S3StorageService;
import com.hyperxconvert.backend.service.VirusScanService;
import com.hyperxconvert.backend.service.FileConversionService;
import org.apache.tika.Tika;
import java.io.File;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileInputStream;

@Slf4j
@Service
public class ConversionWorkerService {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final FileRepository fileRepository;
    private final ConvertLogRepository convertLogRepository;
    // Inject các service phụ: S3, conversion, virus scan (bổ sung sau)

    @Autowired
    private S3StorageService s3StorageService;
    @Autowired
    private VirusScanService virusScanService;
    @Autowired
    private FileConversionService fileConversionService;

    private final Tika tika = new Tika();
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String[] SUPPORTED_FORMATS = {"PDF", "DOCX", "JPG", "PNG", "MP4"};
    private static final int MAX_RETRIES = 3;
    private static final int[] BACKOFF_SECONDS = {2, 4, 8};

    @Value("${aws.sqs.convert-queue}")
    private String convertQueueUrl;
    @Value("${aws.sqs.dlq-convert-queue}")
    private String dlqConvertQueueUrl;

    public ConversionWorkerService(SqsClient sqsClient, ObjectMapper objectMapper,
                                   FileRepository fileRepository, ConvertLogRepository convertLogRepository) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.fileRepository = fileRepository;
        this.convertLogRepository = convertLogRepository;
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
        LocalDateTime startedAt = LocalDateTime.now();
        com.hyperxconvert.backend.entity.File fileEntity = null;
        File inputFile = null;
        File convertedFile = null;
        String errorCode = null;
        int receiveCount = 1;
        try {
            if (msg.attributes().containsKey("ApproximateReceiveCount")) {
                receiveCount = Integer.parseInt(msg.attributes().get("ApproximateReceiveCount"));
            }
            ConvertQueueMessage job = objectMapper.readValue(msg.body(), ConvertQueueMessage.class);
            log.info("[ConversionWorker] Nhận job chuyển đổi: {} (lần thử: {})", job, receiveCount);
            // 1. Lấy file entity từ DB
            Optional<com.hyperxconvert.backend.entity.File> fileOpt = fileRepository.findById(UUID.fromString(job.getFileId()));
            if (fileOpt.isEmpty()) throw new Exception("FILE_NOT_FOUND");
            fileEntity = fileOpt.get();
            // 2. Tải file từ S3
            inputFile = s3StorageService.downloadFile(job.getOriginalPath());
            // 2.1 Nếu chuyển đổi DOCX -> PDF, copy file tạm thành .docx
            File realInputFile = inputFile;
            if ("DOCX".equalsIgnoreCase(fileEntity.getFormatFrom()) && "PDF".equalsIgnoreCase(job.getTargetFormat())) {
                File docxFile = Files.createTempFile("input-", ".docx").toFile();
                Files.copy(inputFile.toPath(), docxFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                realInputFile = docxFile;
                // Kiểm tra file đầu vào thực sự là DOCX
                if (!isDocxFile(realInputFile)) {
                    throw new Exception("INPUT_ERROR: File không phải DOCX hợp lệ");
                }
            }
            // 3. Validate kích thước
            if (realInputFile.length() > MAX_FILE_SIZE) throw new Exception("FILE_TOO_LARGE");
            // 3. Validate định dạng
            String mimeType = tika.detect(realInputFile);
            String format = getFormatFromMimeType(mimeType);
            boolean supported = false;
            for (String f : SUPPORTED_FORMATS) {
                if (f.equalsIgnoreCase(format)) { supported = true; break; }
            }
            if (!supported) throw new Exception("UNSUPPORTED_FORMAT");
            // 4. Quét virus
//            virusScanService.scanFile(realInputFile);
            // 5. Chuyển đổi file
            convertedFile = fileConversionService.convert(realInputFile, fileEntity.getFormatFrom(), job.getTargetFormat());
            // 6. Upload file kết quả lên S3
            String convertedKey = String.format("converted/%s/%s.%s", fileEntity.getUserIp(), fileEntity.getFileId(), job.getTargetFormat().toLowerCase());
            s3StorageService.uploadFile(convertedKey, convertedFile, "application/octet-stream");
            // 7. Cập nhật DB: files, convert_queue_logs
            fileEntity.setStatus("SUCCESS");
            fileEntity.setConvertedPath(convertedKey);
            fileRepository.save(fileEntity);
            ConvertLog logEntry = new ConvertLog(UUID.randomUUID(), fileEntity.getFileId(), fileEntity.getUserIp(), "SUCCESS", startedAt, LocalDateTime.now(), null, LocalDateTime.now());
            convertLogRepository.save(logEntry);
            // 8. Xóa message khỏi queue
            sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(convertQueueUrl).receiptHandle(msg.receiptHandle()).build());
        } catch (Exception e) {
            errorCode = e.getMessage();
            log.error("[ConversionWorker] Lỗi xử lý message: {}", errorCode, e);
            // Retry nếu chưa vượt quá số lần thử
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
                log.warn("[ConversionWorker] Sẽ thử lại job sau {} giây (lần thử: {})", backoff, receiveCount + 1);
                // Không xóa message khỏi queue, SQS sẽ tự động retry
            } else {
                // Gửi vào DLQ (hoặc để SQS chuyển vào DLQ nếu cấu hình RedrivePolicy)
                log.error("[ConversionWorker] Đã vượt quá số lần thử, gửi vào DLQ hoặc để SQS xử lý.");
                // Xóa message khỏi queue chính để tránh lặp vô hạn
                sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(convertQueueUrl).receiptHandle(msg.receiptHandle()).build());
            }
            // Cập nhật DB trạng thái FAILED
            if (fileEntity != null) {
                fileEntity.setStatus("FAILED");
                fileRepository.save(fileEntity);
                String shortErrorCode = (errorCode != null && errorCode.length() > 50) ? errorCode.substring(0, 50) : errorCode;
                ConvertLog logEntry = new ConvertLog(UUID.randomUUID(), fileEntity.getFileId(), fileEntity.getUserIp(), "FAILED", startedAt, LocalDateTime.now(), shortErrorCode, LocalDateTime.now());
                convertLogRepository.save(logEntry);
            }
        } finally {
            // Dọn dẹp file tạm
            if (inputFile != null && inputFile.exists()) inputFile.delete();
            if (convertedFile != null && convertedFile.exists()) convertedFile.delete();
        }
    }

    private String getFormatFromMimeType(String mimeType) {
        switch (mimeType) {
            case "application/pdf": return "PDF";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document": return "DOCX";
            case "application/x-tika-ooxml": return "DOCX"; // Bổ sung mapping cho DOCX
            case "image/jpeg": return "JPG";
            case "image/png": return "PNG";
            case "video/mp4": return "MP4";
            default: return "UNKNOWN";
        }
    }
} 