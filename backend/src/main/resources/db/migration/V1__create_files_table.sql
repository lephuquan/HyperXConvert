CREATE TABLE files (
                       file_id UUID PRIMARY KEY,
                       user_ip VARCHAR(45) NOT NULL,
                       original_path VARCHAR(255) NOT NULL,
                       converted_path VARCHAR(255),
                       format_from VARCHAR(50) NOT NULL,
                       format_to VARCHAR(50) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       upload_time TIMESTAMP NOT NULL,
                       expiry_time TIMESTAMP NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX files_user_ip ON files(user_ip);