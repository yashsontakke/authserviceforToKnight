package com.example.demo.dto;



import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

 @Data // Optional Lombok
public class UpdateProfileRequest {
    private String bio;
    private MultipartFile imageFile; // The uploaded image file

    // --- Generate Getters and Setters ---
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }
}