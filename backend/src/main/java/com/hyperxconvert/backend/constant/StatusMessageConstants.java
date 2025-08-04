package com.hyperxconvert.backend.constant;

import java.util.HashMap;
import java.util.Map;

public class StatusMessageConstants {
    
    public static final Map<String, String> STATUS_MESSAGES = new HashMap<>();
    
    static {
        STATUS_MESSAGES.put("UPLOADED", "File uploaded successfully, waiting for validation");
        STATUS_MESSAGES.put("VALIDATING", "File is being validated");
        STATUS_MESSAGES.put("VALIDATION_FAILED", "File validation failed");
        STATUS_MESSAGES.put("VALIDATED", "File validated successfully, ready for conversion");
        STATUS_MESSAGES.put("CONVERTING", "File is being converted");
        STATUS_MESSAGES.put("CONVERSION_FAILED", "File conversion failed");
        STATUS_MESSAGES.put("CONVERTED", "File converted successfully");
    }
    
    public static String getMessage(String status) {
        return STATUS_MESSAGES.getOrDefault(status, "Unknown status");
    }
} 