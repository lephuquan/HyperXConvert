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

        // Chuyển sang basename chung để hỗ trợ nhiều ngôn ngữ
        messageSource.setBasename("classpath:messages"); // Đổi sang messages để Spring tự động chọn file theo locale
        messageSource.setDefaultEncoding("UTF-8");

        // Không fallback theo hệ điều hành (chỉ dùng file messages_*.properties đúng locale)
        messageSource.setFallbackToSystemLocale(false);

        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        // Tự động lấy locale từ header "Accept-Language" của request
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(new Locale("vi")); // Ngôn ngữ mặc định là tiếng Việt
        return resolver;
    }
}
