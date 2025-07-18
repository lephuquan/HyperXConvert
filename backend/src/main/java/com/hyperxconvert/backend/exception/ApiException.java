package com.hyperxconvert.backend.exception;

public class ApiException extends RuntimeException {
    private final String errorCode;
    private final String messageKey;
    private final Object[] args;

    public ApiException(String errorCode, String messageKey) {
        super(messageKey);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.args = null;
    }
    public ApiException(String errorCode, String messageKey, Object[] args) {
        super(messageKey);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.args = args;
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