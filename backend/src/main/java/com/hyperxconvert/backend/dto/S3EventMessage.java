package com.hyperxconvert.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DTO for S3 event messages from AWS S3 notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3EventMessage {
    @JsonProperty("Records")
    private List<S3Record> records;
    
    // Getter method để đảm bảo compatibility
    public List<S3Record> getRecords() {
        return records;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3Record {
        private String eventVersion;
        private String eventSource;
        private String awsRegion;
        private String eventTime;
        private String eventName;
        private S3Data s3;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3Data {
        private String s3SchemaVersion;
        private String configurationId;
        private S3Bucket bucket;
        private S3Object object;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3Bucket {
        private String name;
        private S3OwnerIdentity ownerIdentity;
        private String arn;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3OwnerIdentity {
        private String principalId;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class S3Object {
        private String key;
        private Long size;
        private String eTag;
        private String sequencer;
    }
} 