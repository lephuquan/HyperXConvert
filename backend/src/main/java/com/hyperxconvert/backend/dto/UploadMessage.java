package com.hyperxconvert.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * DTO for messages from upload-queue SQS.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMessage {
    private UUID fileId;
    private String userIdOrIp;
    private String filePath;
} 