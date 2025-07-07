package com.hyperxconvert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {
    
    @Id
    @Column(name = "file_id")
    private UUID fileId;
    
    @Column(name = "user_ip", nullable = false, length = 45)
    private String userIp;
    
    @Column(name = "original_path", nullable = false, length = 255)
    private String originalPath;
    
    @Column(name = "converted_path", length = 255)
    private String convertedPath;
    
    @Column(name = "format_from", nullable = false, length = 50)
    private String formatFrom;
    
    @Column(name = "format_to", length = 50)
    private String formatTo;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructor with required fields for creating new files
    public File(UUID fileId, String userIp, String originalPath, String formatFrom, 
                String formatTo, String status, LocalDateTime uploadTime, LocalDateTime expiryTime) {
        this.fileId = fileId;
        this.userIp = userIp;
        this.originalPath = originalPath;
        this.formatFrom = formatFrom;
        this.formatTo = formatTo;
        this.status = status;
        this.uploadTime = uploadTime;
        this.expiryTime = expiryTime;
        this.createdAt = LocalDateTime.now();
    }
} 