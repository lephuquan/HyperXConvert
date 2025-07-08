CREATE TABLE convert_logs (
    id UUID PRIMARY KEY,
    file_id UUID REFERENCES files(file_id),
    user_ip VARCHAR(45) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
); 