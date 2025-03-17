package com.hyperxconvert.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Path;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(
            @Value("${aws.access-key-id}") String accessKeyId,
            @Value("${aws.secret-access-key}") String secretAccessKey,
            @Value("${aws.region}") String region) {
        this.region = region;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    public String uploadFileToS3(File file) {
        String fileKey = "converted/" + file.getName();
        Path filePath = file.toPath();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        s3Client.putObject(putObjectRequest, filePath);
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileKey;
    }
}