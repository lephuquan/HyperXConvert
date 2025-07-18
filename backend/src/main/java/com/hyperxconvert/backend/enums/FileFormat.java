package com.hyperxconvert.backend.enums;

public enum FileFormat {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    MP4("mp4", "video/mp4"),
    MP3("mp3", "audio/mpeg"),
    COMPRESSED_PDF("compressed_pdf", null),
    COMPRESSED_VIDEO("compressed_video", null);

    private final String extension;
    private final String contentType;

    FileFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public static FileFormat fromExtension(String ext) {
        if (ext == null) return null;
        for (FileFormat f : values()) {
            if (f.extension != null && f.extension.equalsIgnoreCase(ext)) return f;
        }
        return null;
    }

    public static FileFormat fromContentType(String contentType) {
        if (contentType == null) return null;
        for (FileFormat f : values()) {
            if (f.contentType != null && f.contentType.equalsIgnoreCase(contentType)) return f;
        }
        return null;
    }

    public static FileFormat fromString(String value) {
        if (value == null) return null;
        try {
            return FileFormat.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
} 