package com.hyperxconvert.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Service
public class S3Service {
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3Service(
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.region}") String region,
            AwsCredentialsProvider credentialsProvider,
            @Value("${aws.endpoint:}") String awsEndpoint
    ) {
        S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider);
        if (awsEndpoint != null && !awsEndpoint.isEmpty()) {
            presignerBuilder.endpointOverride(URI.create(awsEndpoint));
        }
        this.s3Presigner = presignerBuilder.build();
        this.bucketName = bucketName;
    }

    public String generatePresignedUploadUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(24))
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }
} 