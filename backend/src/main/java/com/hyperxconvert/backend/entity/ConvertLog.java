package com.hyperxconvert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "convert_queue_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLog {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "user_ip", nullable = false)
    private String userIp;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 