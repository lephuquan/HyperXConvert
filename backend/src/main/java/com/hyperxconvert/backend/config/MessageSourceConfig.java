package com.hyperxconvert.backend.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;


@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // Set basename để Spring tự động chọn file theo locale
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");

        // Cho phép fallback để tránh NoSuchMessageException
        messageSource.setFallbackToSystemLocale(true);
        
        // Set cache duration để reload messages khi cần
        messageSource.setCacheSeconds(3600);

        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Tự động lấy locale từ header "Accept-Language" của request
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(new Locale("en")); // Default language is English
        return resolver;
    }
}
