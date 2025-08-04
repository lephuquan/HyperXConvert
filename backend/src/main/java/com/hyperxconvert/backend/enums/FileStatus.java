package com.hyperxconvert.backend.enums;

public enum FileStatus {
    UPLOADED("UPLOADED"),
    VALIDATING("VALIDATING"),
    VALIDATION_FAILED("VALIDATION_FAILED"),
    VALIDATED("VALIDATED"),
    CONVERTING("CONVERTING"),
    CONVERSION_FAILED("CONVERSION_FAILED"),
    CONVERTED("CONVERTED");
    
    private final String value;
    
    FileStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static FileStatus fromString(String text) {
        if (text == null) {
            return null;
        }
        for (FileStatus status : FileStatus.values()) {
            if (status.value.equalsIgnoreCase(text) || status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
    
    /**
     * Kiểm tra xem status có phải là status lỗi
     */
    public boolean isErrorStatus() {
        return this == VALIDATION_FAILED || this == CONVERSION_FAILED;
    }

} 