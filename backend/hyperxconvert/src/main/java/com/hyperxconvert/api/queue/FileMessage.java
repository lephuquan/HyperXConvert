package com.hyperxconvert.api.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMessage implements Serializable {
    private static final long serialVersionUID = 1L; // Thêm serialVersionUID để đảm bảo tính tương thích

    private String fileId;
    private String originalFilename;
    private String targetFormat;
}