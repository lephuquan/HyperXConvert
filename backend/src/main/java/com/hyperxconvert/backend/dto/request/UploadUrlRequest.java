package com.hyperxconvert.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequest {
    @NotBlank(message = "{validation.fileName.notBlank}")
    private String fileName;

    @NotNull(message = "{validation.fileSize.notNull}")
    @Max(value = 52428800, message = "{validation.fileSize.max}")
    private Long fileSize;

    @NotBlank(message = "{validation.contentType.notBlank}")
    private String contentType;
} 