package com.hyperxconvert.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload a file to S3
     *
     * @param file The file to upload
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                    originalFilename.substring(originalFilename.lastIndexOf(".") + 1) : "";
            String key = "uploads/" + UUID.randomUUID() + "." + extension;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("Uploaded file to S3: {}", key);
            return key;
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    
    /**
     * Upload a file to S3
     *
     * @param file The file to upload
     * @param key The S3 key to use
     * @return The S3 key of the uploaded file
     */
    public String uploadFile(File file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            
            log.info("Uploaded file to S3: {}", key);
            return key;
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Download a file from S3 to a temporary file
     *
     * @param key The S3 key of the file to download
     * @return The downloaded file
     */
    public File downloadToTemp(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            
            String fileName = key.substring(key.lastIndexOf('/') + 1);
            Path tempFile = Files.createTempFile("download-", fileName);
            
            try (InputStream inputStream = s3Object;
                 FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("Downloaded file from S3: {}", key);
            return tempFile.toFile();
        } catch (Exception e) {
            log.error("Error downloading file from S3", e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    /**
     * Generate a pre-signed URL for downloading a file
     *
     * @param key The S3 key of the file
     * @param expirationMinutes The expiration time in minutes
     * @return The pre-signed URL
     */
    public String generatePresignedUrl(String key, int expirationMinutes) {
        try {
            software.amazon.awssdk.services.s3.presigner.S3Presigner presigner = 
                    software.amazon.awssdk.services.s3.presigner.S3Presigner.create();
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = 
                    software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest = 
                    presigner.presignGetObject(presignRequest);
            
            String presignedUrl = presignedRequest.url().toString();
            presigner.close();
            
            log.info("Generated pre-signed URL for S3 key: {}", key);
            return presignedUrl;
        } catch (Exception e) {
            log.error("Error generating pre-signed URL", e);
            throw new RuntimeException("Failed to generate pre-signed URL", e);
        }
    }

    /**
     * Delete a file from S3
     *
     * @param key The S3 key of the file to delete
     */
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("Deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Error deleting file from S3", e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
}
