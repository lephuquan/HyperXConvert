package com.hyperxconvert.backend.repository;

import com.hyperxconvert.backend.entity.ConvertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ConvertLogRepository extends JpaRepository<ConvertLog, UUID> {
    long countByUserIpAndCreatedAtAfter(String userIp, LocalDateTime after);
} 