package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.dto.FileConvertRequest;
import com.hyperxconvert.backend.dto.FileConvertResponse;
import com.hyperxconvert.backend.dto.UploadUrlRequest;
import com.hyperxconvert.backend.dto.UploadUrlResponse;
import com.hyperxconvert.backend.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {
    private final FileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> createPresignedUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            HttpServletRequest httpRequest
    ) {
        UploadUrlResponse response = fileService.createPresignedUploadUrl(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/convert")
    public ResponseEntity<FileConvertResponse> convertFile(@Valid @RequestBody FileConvertRequest request) {
        FileConvertResponse response = fileService.processFileConvertRequest(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{fileId}")
    public ResponseEntity<?> getFileStatus(@PathVariable String fileId) {
        try {
            return ResponseEntity.ok(fileService.getFileStatus(fileId));
        } catch (com.hyperxconvert.backend.exception.ApiException e) {
            logger.error("[FileController] File not found: {}", fileId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new com.hyperxconvert.backend.dto.ErrorResponse("FILE_NOT_FOUND", "File không tồn tại.", "ERROR"));
        } catch (Exception e) {
            logger.error("[FileController] Internal error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.hyperxconvert.backend.dto.ErrorResponse("SYSTEM_ERROR", "Lỗi hệ thống, vui lòng thử lại.", "ERROR"));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String fileId) {
        logger.info("[FileController] Yêu cầu lấy pre-signed URL cho fileId: {}", fileId);
        try {
            return ResponseEntity.ok(fileService.getDownloadUrl(fileId));
        } catch (com.hyperxconvert.backend.exception.ApiException e) {
            logger.error("[FileController] File không tồn tại hoặc chưa được chuyển đổi thành công: {}", fileId);
            logger.error("getDownloadUrl(@PathVariable String fileId)");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new com.hyperxconvert.backend.dto.ErrorResponse("FILE_NOT_READY", "File không tồn tại hoặc chưa được chuyển đổi thành công", "ERROR"));
        } catch (Exception e) {
            logger.error("[FileController] Lỗi hệ thống khi tạo URL tải xuống: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new com.hyperxconvert.backend.dto.ErrorResponse("SYSTEM_ERROR", "Lỗi hệ thống khi tạo URL tải xuống", "ERROR"));
        }
    }
} 