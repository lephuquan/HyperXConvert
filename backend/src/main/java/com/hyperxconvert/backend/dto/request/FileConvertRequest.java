package com.hyperxconvert.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileConvertRequest {
    private String fileId;

    @NotBlank(message = "targetFormat không được để trống")
    private String targetFormat;
} 