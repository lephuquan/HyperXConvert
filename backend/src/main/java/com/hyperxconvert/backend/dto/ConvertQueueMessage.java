package com.hyperxconvert.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertQueueMessage {
    private String fileId;
    private String targetFormat;
    private String originalPath;
} 