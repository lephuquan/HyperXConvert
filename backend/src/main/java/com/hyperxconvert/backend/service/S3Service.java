package com.hyperxconvert.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    /**
     * Tải file từ S3 về local file tạm
     */
    public File downloadFileFromS3(String s3Key) throws Exception {
        log.debug("Downloading file from S3 - key: {}", s3Key);
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
        log.debug("File downloaded successfully - key: {}, size: {} bytes", s3Key, tempFile.length());
        return tempFile;
    }

    /**
     * Upload file từ local lên S3
     */
    public void uploadFileToS3(String s3Key, File file, String contentType) throws Exception {
        log.debug("Uploading file to S3 - key: {}, size: {} bytes, contentType: {}", s3Key, file.length(), contentType);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putObjectRequest, file.toPath());
        log.debug("File uploaded successfully - key: {}", s3Key);
    }

    /**
     * Sinh presigned URL để tải file từ S3 với filename tùy chỉnh
     */
    public String generatePresignedDownloadUrl(String s3Key, String filename, Duration expiry) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(b -> b.bucket(bucketName).key(s3Key)
                        .responseContentDisposition("attachment; filename=\"" + filename + "\""))
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Sinh presigned URL để upload file lên S3
     */
    public String generatePresignedUploadUrl(String s3Key, String contentType, Duration expiry) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }
} 