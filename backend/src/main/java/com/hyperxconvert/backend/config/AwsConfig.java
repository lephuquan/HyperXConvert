package com.hyperxconvert.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() { // Cung cấp thông tin đăng nhập AWS
        // Tự động lấy credentials từ môi trường hoặc ~/.aws/credentials
        return DefaultCredentialsProvider.create();
        /* DefaultCredentialsProvider.create() tự động tìm:
         * 1. Biến môi trường AWS_ACCESS_KEY_ID và AWS_SECRET_ACCESS_KEY
         * 2. Tập tin ~/.aws/credentials
         * 3. IAM role nếu chạy trên EC2 hoặc ECS
         */
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) { // Cấu hình client để giao tiếp với S3
        return S3Client.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) { // Cấu hình client để làm việc với SQS (Simple Queue Service)
        return SqsClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            AwsCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String region
    ) { // Cấu hình để sinh Presigned URL
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
        /* Presigned URL thường dùng để:
         * 1. Cho phép người dùng tải lên hoặc tải xuống file từ S3 mà không cần cung cấp thông tin đăng nhập AWS.
         * 2. Giới hạn thời gian truy cập để bảo mật hơn.
         */
    }
}
