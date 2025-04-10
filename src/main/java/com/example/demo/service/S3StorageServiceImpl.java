package com.example.demo.service;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // For filename cleaning
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException; // Base SDK exception
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
// Import S3 specific exceptions if needed for finer-grained handling
// import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service // Mark as a Spring service bean
public class S3StorageServiceImpl implements S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client s3Client; // Inject the configured S3Client bean

    @Value("${aws.s3.bucket-name}") // Inject bucket name from properties
    private String bucketName;
    
    @Value("${aws.region}")
    private String awsRegion;

     // Constructor injection
    public S3StorageServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(MultipartFile file) throws IOException, IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to upload a null or empty file.");
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        // Clean the filename and generate a unique key
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String fileExtension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            fileExtension = originalFilename.substring(lastDot);
        }
        // Create a unique key using UUID - include a directory structure if desired
        String key = "profile-images/" + UUID.randomUUID().toString() + fileExtension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType()) // Pass content type
                    // Optional: Set ACL if making objects publically readable - use with caution
                    // .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            // Upload the file content
            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Successfully uploaded file '{}' to S3 bucket '{}' with key '{}'", originalFilename, bucketName, key);
            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, awsRegion /* Need region here */, key);
            log.info("File uploaded successfully to S3. URL: {}", url);
            return url;

        } catch (SdkException e) { // Catch AWS SDK specific exceptions
            log.error("Error uploading file to S3. Bucket: {}, Key: {}. Error: {}", bucketName, key, e.getMessage(), e);
            // Re-throw as a runtime exception or a custom exception
            throw new RuntimeException("S3 upload failed: " + e.getMessage(), e);
        }
        // IOException is already declared in the method signature for file.getInputStream()
    }

    @Override
    public void deleteFile(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            log.warn("Attempted to delete file with null or empty key.");
            return; // Or throw exception? Depends on requirements.
        }
        log.info("Attempting to delete file from S3. Bucket: {}, Key: {}", bucketName, fileKey);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3 with key: {}", fileKey);

        } catch (SdkException e) {
            log.error("Error deleting file from S3. Bucket: {}, Key: {}. Error: {}", bucketName, fileKey, e.getMessage(), e);
            // Depending on requirements, you might re-throw this
             throw new RuntimeException("S3 delete failed: " + e.getMessage(), e);
        }
    }
}