-- Migration: Thêm các cột còn thiếu vào bảng files để đồng bộ với entity File.java
ALTER TABLE files
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS format_to VARCHAR(50);