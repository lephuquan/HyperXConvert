package com.hyperxconvert.api.queue;

import com.hyperxconvert.api.config.RabbitMQConfig;
import com.hyperxconvert.api.service.FileConversionService;
import com.hyperxconvert.api.service.S3StorageService;
import com.hyperxconvert.api.service.WordToPdfConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class FileProcessor {
    private final FileConversionService conversionService;
    private final S3StorageService s3StorageService;
    private final WordToPdfConverter wordToPdfConverter;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void processFile(FileMessage message) {
        try {
            String inputFilePath = message.getOriginalFilename();
            File convertedFile;

            // Nếu tệp là .docx, chuyển đổi sang PDF trước
            if (inputFilePath.endsWith(".docx")) {
                convertedFile = wordToPdfConverter.convertDocxToPdf(inputFilePath);
            } else {
                // Chuyển đổi tệp bằng ImageMagick
                convertedFile = conversionService.convertFile(inputFilePath, message.getTargetFormat());
            }

            // Tải tệp đã chuyển đổi lên S3
            String fileUrl = s3StorageService.uploadFileToS3(convertedFile);
            System.out.println("File đã xử lý xong: " + fileUrl);
        } catch (Exception e) {
            System.err.println("Lỗi xử lý file: " + e.getMessage());
        }
    }
}