package com.hyperxconvert.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileConvertResponse {
    private String jobId;
    private String status;
} 