-- Migration for audit and timestamp refactor
ALTER TABLE convert_queue_logs
    DROP COLUMN IF EXISTS started_at,
    ADD COLUMN updated_at TIMESTAMP;

ALTER TABLE files
    DROP COLUMN IF EXISTS upload_time,
    ADD COLUMN updated_at TIMESTAMP; 