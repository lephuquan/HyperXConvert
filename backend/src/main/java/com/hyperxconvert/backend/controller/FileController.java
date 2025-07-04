package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.dto.ErrorResponse;
import com.hyperxconvert.backend.dto.UploadUrlRequest;
import com.hyperxconvert.backend.dto.UploadUrlResponse;
import com.hyperxconvert.backend.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/file")
@Validated
public class FileController {
    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @GetMapping("/upload-url")
    public ResponseEntity<?> getUploadUrl(
            @RequestParam @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Filename contains invalid characters") String filename,
            @RequestParam @NotNull @Max(value = 52428800, message = "File size exceeds 50MB limit") Long fileSize,
            HttpServletRequest request
    ) {
        UploadUrlRequest uploadUrlRequest = new UploadUrlRequest(filename, fileSize);
        String userIp = fileUploadService.getUserIpAddress(request);
        UploadUrlResponse response = fileUploadService.generateUploadUrl(uploadUrlRequest, userIp);
        return ResponseEntity.ok(response);
    }
} 