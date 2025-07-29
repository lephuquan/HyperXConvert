package com.hyperxconvert.backend.controller;

import com.hyperxconvert.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {
    
    @Autowired
    private MessageSource messageSource;
    
    @Autowired
    private LocaleResolver localeResolver;
    
    /**
     * Test cơ bản message source với locale hiện tại
     */
    @GetMapping("/messages") // Kiểm tra message source hoạt động đúng
    public Map<String, Object> testMessages(HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("currentLocale", locale.toString());
        
        // Test các message key chính
        Map<String, String> messages = new HashMap<>();
        messages.put("error.file.too.large", messageSource.getMessage("error.file.too.large", null, locale));
        messages.put("error.unsupported.format", messageSource.getMessage("error.unsupported.format", null, locale));
        messages.put("error.internal", messageSource.getMessage("error.internal", null, locale));
        messages.put("error.file.not.found", messageSource.getMessage("error.file.not.found", null, locale));
        
        result.put("messages", messages);
        return result;
    }
    
    /**
     * Test fallback mechanism cho các locale khác nhau
     */
    @GetMapping("/fallback-test") // Kiểm tra fallback mechanism hoạt động đúng
    public Map<String, Object> testFallback(HttpServletRequest request) {
        Locale locale = localeResolver.resolveLocale(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("requestedLocale", locale.toString());
        
        try {
            String message = messageSource.getMessage("error.file.too.large", null, locale);
            result.put("message", message);
        } catch (Exception e) {
            result.put("message", "Message not found for locale: " + locale);
            result.put("error", e.getMessage());
        }
        
        // Test với các locale khác nhau để kiểm tra fallback
        Map<String, String> fallbackTests = new HashMap<>();
        String[] testLocales = {"en", "vi", "fr", "de", "ja"};
        
        for (String lang : testLocales) {
            try {
                String message = messageSource.getMessage("error.file.too.large", null, new Locale(lang));
                fallbackTests.put(lang, message);
            } catch (Exception e) {
                fallbackTests.put(lang, "Message not found for locale: " + lang);
            }
        }
        
        result.put("fallbackTests", fallbackTests);
        return result;
    }
    
    /**
     * Test error handling với message source
     */
    @GetMapping("/error-simulation") // Kiểm tra error handling hoạt động đúng
    public void simulateError() {
        throw new ApiException("FILE_TOO_LARGE", "error.file.too.large");
    }
} 