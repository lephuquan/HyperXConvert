package com.hyperxconvert.backend.constant;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.hyperxconvert.backend.enums.FileFormat;

public class FileFormatConstants {
    // Danh sách các format gốc (dùng cho upload validation)
    public static final List<FileFormat> UPLOAD_FORMATS = Arrays.asList(
        FileFormat.PDF, FileFormat.DOCX, FileFormat.JPG, FileFormat.PNG, FileFormat.MP4
    );
    
    // Danh sách các content type được hỗ trợ cho upload
    public static final List<String> ALLOWED_CONTENT_TYPES = UPLOAD_FORMATS.stream()
        .map(FileFormat::getContentType)
        .filter(ct -> ct != null)
        .collect(Collectors.toList());

    // Danh sách các extension được hỗ trợ cho upload
    public static final List<String> ALLOWED_EXTENSIONS = UPLOAD_FORMATS.stream()
        .map(FileFormat::getExtension)
        .filter(ext -> ext != null)
        .collect(Collectors.toList());

    // Danh sách tất cả format (bao gồm cả compressed) - dùng cho conversion
    public static final List<FileFormat> SUPPORTED_FORMATS = Arrays.asList(FileFormat.values());

    // Map extension to content type (chỉ dùng cho upload formats)
    public static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = UPLOAD_FORMATS.stream()
        .filter(f -> f.getExtension() != null && f.getContentType() != null)
        .collect(Collectors.toMap(FileFormat::getExtension, FileFormat::getContentType));

    // Map content type to format (chỉ dùng cho upload formats)
    public static final Map<String, FileFormat> MIME_TYPE_TO_FORMAT = UPLOAD_FORMATS.stream()
        .filter(f -> f.getContentType() != null)
        .collect(Collectors.toMap(FileFormat::getContentType, f -> f));
} 