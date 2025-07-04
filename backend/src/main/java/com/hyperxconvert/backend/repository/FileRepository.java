package com.hyperxconvert.backend.repository;

import com.hyperxconvert.backend.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    
    /**
     * Count files uploaded by a specific IP address within a time range
     */
    @Query("SELECT COUNT(f) FROM File f WHERE f.userIp = :userIp AND f.uploadTime >= :startTime")
    long countByUserIpAndUploadTimeAfter(@Param("userIp") String userIp, @Param("startTime") LocalDateTime startTime);
    
    /**
     * Find files by user IP address
     */
    List<File> findByUserIpOrderByUploadTimeDesc(String userIp);
    
    /**
     * Find expired files
     */
    @Query("SELECT f FROM File f WHERE f.expiryTime < :currentTime")
    List<File> findExpiredFiles(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find files by status
     */
    List<File> findByStatus(String status);
} 