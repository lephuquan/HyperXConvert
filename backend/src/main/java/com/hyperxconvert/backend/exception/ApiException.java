package com.hyperxconvert.backend.exception;

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final String messageKey;
    private final Object[] args;

    public ApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.messageKey = null;
        this.args = null;
    }

    public String getErrorCode() {
        return errorCode;
    }
    public String getMessageKey() {
        return messageKey;
    }
    public Object[] getArgs() {
        return args;
    }
} 