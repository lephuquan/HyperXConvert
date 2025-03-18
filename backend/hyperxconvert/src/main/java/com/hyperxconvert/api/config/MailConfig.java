package com.hyperxconvert.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.primary.host:smtp.sendgrid.net}")
    private String primaryHost;

    @Value("${spring.mail.primary.port:587}")
    private int primaryPort;

    @Value("${spring.mail.primary.username:apikey}")
    private String primaryUsername;

    @Value("${spring.mail.primary.password:${SENDGRID_API_KEY:sendgrid-api-key}}")
    private String primaryPassword;

    @Value("${spring.mail.fallback.host:smtp.gmail.com}")
    private String fallbackHost;

    @Value("${spring.mail.fallback.port:587}")
    private int fallbackPort;

    @Value("${spring.mail.fallback.username:${GMAIL_USERNAME:your-gmail@gmail.com}}")
    private String fallbackUsername;

    @Value("${spring.mail.fallback.password:${GMAIL_PASSWORD:your-gmail-password}}")
    private String fallbackPassword;

    @Primary
    @Bean(name = "primaryMailSender")
    public JavaMailSender primaryMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(primaryHost);
        mailSender.setPort(primaryPort);
        mailSender.setUsername(primaryUsername);
        mailSender.setPassword(primaryPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    @Bean(name = "fallbackMailSender")
    public JavaMailSender fallbackMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(fallbackHost);
        mailSender.setPort(fallbackPort);
        mailSender.setUsername(fallbackUsername);
        mailSender.setPassword(fallbackPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
