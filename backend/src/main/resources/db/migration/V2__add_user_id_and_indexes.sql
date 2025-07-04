-- Add user_id column to files table for future user authentication
ALTER TABLE files ADD COLUMN user_id UUID;

-- Add indexes for better performance
CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_user_ip_upload_time ON files(user_ip, upload_time);
CREATE INDEX idx_files_expiry_time ON files(expiry_time);
CREATE INDEX idx_files_status ON files(status);

-- Rename existing index to follow naming convention
DROP INDEX IF EXISTS files_user_ip;
CREATE INDEX idx_files_user_ip ON files(user_ip); 