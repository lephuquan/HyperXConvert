package com.hyperxconvert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class File {
    
    @Id
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Column(name = "user_ip")
    private String userIp;
    
    @Column(name = "original_path")
    private String originalPath;
    
    @Column(name = "converted_path")
    private String convertedPath;
    
    @Column(name = "format_from")
    private String formatFrom;
    
    @Column(name = "format_to")
    private String formatTo;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor with required fields for creating new files
    public File(UUID fileId, String userIp, String originalPath, String formatFrom, String formatTo, String status, LocalDateTime expiryTime) {
        this.fileId = fileId;
        this.userIp = userIp;
        this.originalPath = originalPath;
        this.formatFrom = formatFrom;
        this.formatTo = formatTo;
        this.status = status;
        this.expiresAt = expiryTime;
        this.createdAt = LocalDateTime.now();
    }
} 