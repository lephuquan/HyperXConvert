package com.hyperxconvert.api.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Message for file conversion queue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMessage implements Serializable {
    private String conversionId;
    private String s3Path;
    private String sourceFormat;
    private String targetFormat;
    
    public FileMessage(String conversionId, String s3Path, String targetFormat) {
        this.conversionId = conversionId;
        this.s3Path = s3Path;
        this.targetFormat = targetFormat;
    }
}
