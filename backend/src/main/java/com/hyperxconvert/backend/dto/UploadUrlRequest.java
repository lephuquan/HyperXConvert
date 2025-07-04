package com.hyperxconvert.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {
    
    @NotBlank(message = "Filename is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Filename contains invalid characters")
    private String filename;
    
    @NotNull(message = "File size is required")
    @Max(value = 52428800, message = "File size exceeds 50MB limit")
    private Long fileSize;
} 