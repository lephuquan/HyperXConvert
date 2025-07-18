package com.hyperxconvert.backend.enums;

public enum FileStatus {
    UPLOADED("UPLOADED"),
    QUEUED_AND_VALIDATED("QUEUED_AND_VALIDATED"),
    QUEUED_AND_CONVERTED("QUEUED_AND_CONVERTED"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");
    
    private final String value;
    
    FileStatus(String value) {
        this.value = value;
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