package com.hyperxconvert.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlResponse {
    
    private String uploadUrl;
    private String fileKey;
    private UUID fileId;
} 