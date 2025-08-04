package com.hyperxconvert.backend.util;

import java.util.UUID;

public class FilenameUtils {
    
    /**
     * Sanitize filename to be safe for file system and S3
     * Remove or replace unsafe characters
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // Replace unsafe characters with underscore
        String sanitized = filename
                .replaceAll("[<>:\"/\\\\|?*]", "_")  // Windows unsafe chars
                .replaceAll("[\\x00-\\x1f]", "_")    // Control characters
                .replaceAll("\\s+", "_")             // Multiple spaces to single underscore
                .trim();
        
        // Ensure filename is not too long (S3 limit is 1024 bytes for key)
        if (sanitized.length() > 200) {
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                String name = sanitized.substring(0, lastDot);
                String ext = sanitized.substring(lastDot);
                sanitized = name.substring(0, 200 - ext.length()) + ext;
            } else {
                sanitized = sanitized.substring(0, 200);
            }
        }
        
        return sanitized.isEmpty() ? "file_" + UUID.randomUUID().toString().substring(0, 8) : sanitized;
    }
    
    /**
     * Extract base filename without extension
     */
    public static String getBaseFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "converted_file";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }
    
    /**
     * Get filename with new extension
     */
    public static String getFilenameWithExtension(String originalFilename, String newExtension) {
        String baseFilename = getBaseFilename(originalFilename);
        return baseFilename + "." + newExtension;
    }

    /**
     * Generate converted or compressed filename with format:
     * originalName-converted-YYYYMMDD-HHmm.ext or originalName-compressed-YYYYMMDD-HHmm.ext
     * Accepts a java.util.Locale to determine timezone (Vietnam/UTC)
     */
    public static String getProcessedFilename(String originalFilename, String action, String extension, java.util.Locale locale) {
        String base = getBaseFilename(originalFilename);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
        java.time.ZoneId zoneId = (locale != null && (locale.getLanguage().equalsIgnoreCase("vi") || locale.getCountry().equalsIgnoreCase("VN")))
            ? java.time.ZoneId.of("Asia/Ho_Chi_Minh")
            : java.time.ZoneOffset.UTC;
        String timestamp = java.time.ZonedDateTime.now(zoneId).format(dtf);
        return base + "-" + action + "-" + timestamp + "." + extension;
    }
}
