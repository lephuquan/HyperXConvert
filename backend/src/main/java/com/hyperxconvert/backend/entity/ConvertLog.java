package com.hyperxconvert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "convert_logs")
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;
} 