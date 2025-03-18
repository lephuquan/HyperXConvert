package com.hyperxconvert.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender primaryMailSender;
    private final JavaMailSender fallbackMailSender;
    
    private final AtomicBoolean useFallback = new AtomicBoolean(false);
    
    @Value("${spring.mail.primary.username}")
    private String primaryFromEmail;
    
    @Value("${spring.mail.fallback.username}")
    private String fallbackFromEmail;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void sendEmail(String to, String subject, String content) {
        if (useFallback.get()) {
            try {
                sendWithFallback(to, subject, content);
            } catch (Exception e) {
                log.error("Email fallback failed", e);
                throw new RuntimeException("Failed to send email", e);
            }
        } else {
            try {
                sendWithPrimary(to, subject, content);
            } catch (Exception e) {
                // Switch to fallback
                useFallback.set(true);
                log.error("Primary email service failed, switching to fallback", e);
                sendEmail(to, subject, content); // Retry with fallback
            }
        }
    }
    
    private void sendWithPrimary(String to, String subject, String content) throws MessagingException {
        MimeMessage message = primaryMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true for HTML
        helper.setFrom(primaryFromEmail);
        primaryMailSender.send(message);
        log.info("Email sent to {} using primary mail sender", to);
    }
    
    private void sendWithFallback(String to, String subject, String content) throws MessagingException {
        MimeMessage message = fallbackMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        helper.setFrom(fallbackFromEmail);
        fallbackMailSender.send(message);
        log.info("Email sent to {} using fallback mail sender", to);
    }
    
    public void sendWelcomeEmail(String to, String fullName) {
        String subject = "Welcome to HyperXConvert!";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                "<h2>Welcome to HyperXConvert, %s!</h2>" +
                "<p>Thank you for joining our platform. We're excited to have you on board!</p>" +
                "<p>With HyperXConvert, you can:</p>" +
                "<ul>" +
                "<li>Convert files between various formats</li>" +
                "<li>Access your converted files from anywhere</li>" +
                "<li>Enjoy high-quality conversions</li>" +
                "</ul>" +
                "<p>If you have any questions, feel free to contact our support team.</p>" +
                "<p>Best regards,<br>The HyperXConvert Team</p>" +
                "</div>",
                fullName);
        
        sendEmail(to, subject, content);
    }
    
    public void sendConversionCompleteEmail(String to, String fullName, String fileName, String downloadUrl) {
        String subject = "Your File Conversion is Complete";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                "<h2>Your File Conversion is Complete</h2>" +
                "<p>Hello %s,</p>" +
                "<p>We're pleased to inform you that your file <strong>%s</strong> has been successfully converted.</p>" +
                "<p>You can download your converted file by clicking the button below:</p>" +
                "<div style='text-align: center; margin: 20px 0;'>" +
                "<a href='%s' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;'>Download File</a>" +
                "</div>" +
                "<p>The download link will expire in 7 days, so please download your file as soon as possible.</p>" +
                "<p>Thank you for using HyperXConvert!</p>" +
                "<p>Best regards,<br>The HyperXConvert Team</p>" +
                "</div>",
                fullName, fileName, downloadUrl);
        
        sendEmail(to, subject, content);
    }
    
    public void sendSubscriptionExpiringEmail(String to, String fullName, String planType, LocalDateTime expiryDate) {
        String subject = "Your HyperXConvert Subscription is Expiring Soon";
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                "<h2>Your Subscription is Expiring Soon</h2>" +
                "<p>Hello %s,</p>" +
                "<p>This is a friendly reminder that your <strong>%s</strong> subscription will expire on <strong>%s</strong>.</p>" +
                "<p>To continue enjoying our services without interruption, please renew your subscription before the expiry date.</p>" +
                "<div style='text-align: center; margin: 20px 0;'>" +
                "<a href='https://hyperxconvert.com/subscriptions' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;'>Renew Subscription</a>" +
                "</div>" +
                "<p>If you have any questions or need assistance, please don't hesitate to contact our support team.</p>" +
                "<p>Thank you for choosing HyperXConvert!</p>" +
                "<p>Best regards,<br>The HyperXConvert Team</p>" +
                "</div>",
                fullName, planType, expiryDate.format(DATE_FORMATTER));
        
        sendEmail(to, subject, content);
    }
    
    public void sendPasswordResetEmail(String to, String fullName, String resetToken) {
        String subject = "Reset Your HyperXConvert Password";
        String resetUrl = "https://hyperxconvert.com/reset-password?token=" + resetToken;
        
        String content = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px;'>" +
                "<h2>Reset Your Password</h2>" +
                "<p>Hello %s,</p>" +
                "<p>We received a request to reset your password. If you didn't make this request, you can ignore this email.</p>" +
                "<p>To reset your password, click the button below:</p>" +
                "<div style='text-align: center; margin: 20px 0;'>" +
                "<a href='%s' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px;'>Reset Password</a>" +
                "</div>" +
                "<p>This link will expire in 24 hours.</p>" +
                "<p>If the button doesn't work, you can copy and paste the following URL into your browser:</p>" +
                "<p>%s</p>" +
                "<p>Best regards,<br>The HyperXConvert Team</p>" +
                "</div>",
                fullName, resetUrl, resetUrl);
        
        sendEmail(to, subject, content);
    }
}
