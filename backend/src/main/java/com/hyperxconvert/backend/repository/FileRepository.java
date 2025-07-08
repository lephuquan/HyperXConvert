package com.hyperxconvert.backend.repository;

import com.hyperxconvert.backend.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<File, UUID> {
    // Có thể thêm các truy vấn custom nếu cần
} 