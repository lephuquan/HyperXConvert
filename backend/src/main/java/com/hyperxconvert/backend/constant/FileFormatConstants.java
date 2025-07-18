package com.hyperxconvert.backend.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.hyperxconvert.backend.enums.FileFormat;

public class FileFormatConstants {
    // Danh sách các content type được hỗ trợ (dùng cho cả validate upload và validate convert)
    public static final List<String> ALLOWED_CONTENT_TYPES = Arrays.stream(FileFormat.values())
        .map(FileFormat::getContentType)
        .filter(ct -> ct != null)
        .collect(Collectors.toList());

    public static final List<String> ALLOWED_EXTENSIONS = Arrays.stream(FileFormat.values())
        .map(FileFormat::getExtension)
        .filter(ext -> ext != null)
        .collect(Collectors.toList());

    public static final List<FileFormat> SUPPORTED_FORMATS = Arrays.asList(FileFormat.values());

    public static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Arrays.stream(FileFormat.values())
        .filter(f -> f.getExtension() != null && f.getContentType() != null)
        .collect(Collectors.toMap(FileFormat::getExtension, FileFormat::getContentType));

    public static final Map<String, FileFormat> MIME_TYPE_TO_FORMAT = Arrays.stream(FileFormat.values())
        .filter(f -> f.getContentType() != null)
        .collect(Collectors.toMap(FileFormat::getContentType, f -> f));
} 