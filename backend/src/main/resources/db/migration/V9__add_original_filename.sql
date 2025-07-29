-- Migration: Thêm cột original_filename để lưu tên file gốc từ người dùng
ALTER TABLE files ADD COLUMN original_filename VARCHAR(255); 