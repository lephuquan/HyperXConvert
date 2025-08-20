package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.dto.request.FileConvertRequest;
import com.hyperxconvert.backend.dto.response.FileConvertResponse;
import com.hyperxconvert.backend.dto.request.UploadUrlRequest;
import com.hyperxconvert.backend.dto.response.FileConvertedInfoResponse;
import com.hyperxconvert.backend.dto.response.FileStatusResponse;
import com.hyperxconvert.backend.dto.response.UploadUrlResponse;
import com.hyperxconvert.backend.service.FileManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {
    private final FileManagementService fileService;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public FileController(FileManagementService fileService) {
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
    public ResponseEntity<FileStatusResponse> getFileStatus(@PathVariable String fileId, HttpServletRequest request) {
        java.util.Locale locale = request.getLocale();
        return ResponseEntity.ok(fileService.getFileStatus(fileId, locale));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String fileId, HttpServletRequest request) {
        final String method = "GetDownloadUrl";
        logger.info("[{}] Request to generate pre-signed download URL for fileId: {}", method, fileId);
        java.util.Locale locale = request.getLocale();
        return ResponseEntity.ok(fileService.getDownloadUrl(fileId, locale));
    }

    @GetMapping("/converted-info/{fileId}")
    public ResponseEntity<FileConvertedInfoResponse> getConvertedFileInfo(@PathVariable String fileId, HttpServletRequest request) {
        var file = fileService.getFileEntity(fileId);
        java.util.Locale locale = request.getLocale();
        java.time.LocalDateTime expiresAt = file.getExpiresAt();
        // Nếu locale là Việt Nam thì cộng thêm 7 tiếng (UTC+7)
        if (locale != null && (locale.getLanguage().equalsIgnoreCase("vi") || locale.getCountry().equalsIgnoreCase("VN"))) {
            expiresAt = expiresAt != null ? expiresAt.plusHours(7) : null;
        }
        FileConvertedInfoResponse response = new FileConvertedInfoResponse(
            file.getConvertedFileSize(),
            expiresAt
        );
        return ResponseEntity.ok(response);
    }
}
