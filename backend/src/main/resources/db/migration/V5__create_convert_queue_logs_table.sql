CREATE TABLE convert_queue_logs (
    id UUID PRIMARY KEY,
    file_id UUID REFERENCES files(file_id),
    user_ip VARCHAR(45) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    error_code VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
); 