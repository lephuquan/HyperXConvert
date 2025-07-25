package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.dto.request.FileConvertRequest;
import com.hyperxconvert.backend.dto.response.FileConvertResponse;
import com.hyperxconvert.backend.dto.request.UploadUrlRequest;
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
    public ResponseEntity<?> getFileStatus(@PathVariable String fileId) {
    return ResponseEntity.ok(fileService.getFileStatus(fileId));
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> getDownloadUrl(@PathVariable String fileId) {
        final String method = "GetDownloadUrl";
        logger.info("[{}] Request to generate pre-signed download URL for fileId: {}", method, fileId);
        return ResponseEntity.ok(fileService.getDownloadUrl(fileId));
    }
} 