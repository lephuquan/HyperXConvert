package com.hyperxconvert.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conversions")
public class Conversion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private String originalFileName;
    private Long originalFileSize;
    private String originalFormat;
    private String targetFormat;
    
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private ConversionStatus status;
    
    private String s3OriginalPath;
    private String s3ConvertedPath;
    private LocalDateTime expiryDate;
    
    private Integer creditsUsed;
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum ConversionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    }
}
