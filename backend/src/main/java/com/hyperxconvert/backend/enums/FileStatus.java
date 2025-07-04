package com.hyperxconvert.backend.enums;

public enum FileStatus {
    PENDING("PENDING"),
    UPLOADED("UPLOADED"),
    CONVERTING("CONVERTING"),
    CONVERTED("CONVERTED"),
    FAILED("FAILED"),
    EXPIRED("EXPIRED");
    
    private final String value;
    
    FileStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static FileStatus fromString(String text) {
        for (FileStatus status : FileStatus.values()) {
            if (status.value.equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
} 