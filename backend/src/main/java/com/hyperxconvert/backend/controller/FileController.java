package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.dto.FileConvertRequest;
import com.hyperxconvert.backend.dto.FileConvertResponse;
import com.hyperxconvert.backend.dto.UploadUrlRequest;
import com.hyperxconvert.backend.dto.UploadUrlResponse;
import com.hyperxconvert.backend.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {
    private final FileService fileService;

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
} 