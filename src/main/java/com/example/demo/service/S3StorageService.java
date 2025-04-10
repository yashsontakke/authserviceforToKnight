package com.example.demo.service;



import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface S3StorageService {

    /**
     * Uploads a file to the configured S3 bucket.
     *
     * @param file The MultipartFile representing the file to upload.
     * @return The unique S3 object key (filename in the bucket) assigned to the uploaded file.
     * @throws IOException If an error occurs reading the file or during the upload process.
     * @throws IllegalArgumentException If the file is null or empty.
     */
    String uploadFile(MultipartFile file) throws IOException;

    /**
     * Deletes a file from the configured S3 bucket using its key.
     * (Optional - Implement if you need to delete old profile pictures)
     *
     * @param fileKey The unique S3 object key of the file to delete.
     */
    void deleteFile(String fileKey);

}