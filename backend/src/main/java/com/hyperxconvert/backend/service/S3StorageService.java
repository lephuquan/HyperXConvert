package com.hyperxconvert.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
public class S3StorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public File downloadFile(String s3Key) throws Exception {
        File tempFile = Files.createTempFile("download-", "-s3").toFile();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        try (InputStream s3is = s3Client.getObject(getObjectRequest);
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] read_buf = new byte[4096];
            int read_len;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
        }
        return tempFile;
    }

    public void uploadFile(String s3Key, File file, String contentType) throws Exception {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putObjectRequest, file.toPath());
    }

    public String generatePresignedUrl(String s3Key, Duration expiry) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(b -> b.bucket(bucketName).key(s3Key))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
} 