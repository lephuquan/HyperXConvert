package com.hyperxconvert.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
    private String fileId;
    private String uploadUrl;
    private String status;
    private String expiresAt;
} 