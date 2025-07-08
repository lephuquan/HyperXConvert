package com.hyperxconvert.backend.exception;

import com.hyperxconvert.backend.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldError() != null ?
                ex.getBindingResult().getFieldError().getDefaultMessage() : messageSource.getMessage("error.validation", null, Locale.getDefault());
        ErrorResponse errorResponse = new ErrorResponse("VALIDATION_ERROR", message, "ERROR");
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        String message = ex.getMessageKey() != null ?
                messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), Locale.getDefault()) : ex.getMessage();
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode(), message, "ERROR");
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception ex) {
        String message = messageSource.getMessage("error.internal", null, Locale.getDefault());
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR", message, "ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 