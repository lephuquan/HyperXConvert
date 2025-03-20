package com.hyperxconvert.api.service;

import com.hyperxconvert.api.queue.RabbitMQSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final RabbitMQSender rabbitMQSender;

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "pdf", "docx", "txt", "rtf", // Documents
            "xlsx", "csv",               // Spreadsheets
            "png", "jpg", "gif", "svg", "webp", // Images
            "mp3", "wav", "aac", "flac", // Audio
            "mp4", "avi", "mov", "webm", // Video
            "zip", "rar"                 // Others
    );

    public List<String> uploadAndProcess(MultipartFile[] files, String targetFormat) {
        // Kiểm tra số lượng tệp tải lên
        if (files.length > 5) throw new IllegalArgumentException("Tối đa 5 file/lần");

        // Kiểm tra định dạng đích có được hỗ trợ không
        if (!SUPPORTED_FORMATS.contains(targetFormat.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng không được hỗ trợ: " + targetFormat);
        }

        List<String> fileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            // Kiểm tra dung lượng tệp
            if (file.getSize() > 100 * 1024 * 1024) {
                throw new IllegalArgumentException("Dung lượng file quá lớn!");
            }

            // Lưu tệp tải lên vào thư mục tạm
            File tempFile = saveFileToTempDirectory(file);

            // Gửi thông tin tệp qua RabbitMQ
            String fileId = UUID.randomUUID().toString();
            rabbitMQSender.sendMessage(new com.hyperxconvert.api.queue.FileMessage(
                fileId, tempFile.getAbsolutePath(), getFileExtension(file.getOriginalFilename()), targetFormat));
            fileIds.add(fileId);
        }
        return fileIds;
    }

    /**
     * Lưu tệp tải lên vào thư mục tạm
     *
     * @param file Tệp tải lên từ người dùng
     * @return Đối tượng File đại diện cho tệp tạm
     */
    private File saveFileToTempDirectory(MultipartFile file) {
        try {
            // Xử lý tên tệp để loại bỏ ký tự không hợp lệ
            String sanitizedFileName = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");

            // Tạo tệp tạm trong thư mục hệ thống
            File tempFile = File.createTempFile("upload_", "_" + sanitizedFileName);
            file.transferTo(tempFile); // Lưu nội dung tệp tải lên vào tệp tạm
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi lưu tệp: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get file extension from filename
     *
     * @param filename The filename
     * @return The file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
