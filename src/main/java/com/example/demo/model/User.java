package com.example.demo.model;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
// Import necessary classes like LocalDate, LocalDateTime

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID; // For generating IDs

@DynamoDbBean // Marks this as a bean for the Enhanced Client
public class User {

    private String userId; // Partition Key - using String UUID is common
    private String googleId; // Need to query by this -> GSI
    private String email;    // Need to query by this -> GSI
    private String name;
    private String profileImagePath;
    private LocalDate dateOfBirth; // Store as String or Number, or use Converter
    private String gender;
    private String bio;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Add roles field if needed

    // --- Partition Key ---
    @DynamoDbPartitionKey // Define the primary partition key
    @DynamoDbAttribute("userId") // Optional: explicit attribute name
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // --- Example Global Secondary Index (GSI) for googleId lookup ---
    // NOTE: This index ("googleId-index") must ALSO be created on your DynamoDB table in AWS!
    @DynamoDbSecondaryPartitionKey(indexNames = "googleId-index")
    @DynamoDbAttribute("googleId")
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    // --- Example Global Secondary Index (GSI) for email lookup ---
    // NOTE: This index ("email-index") must ALSO be created on your DynamoDB table in AWS!
    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // --- Other attributes ---
    // Getters and Setters for name, profileImagePath, dateOfBirth, gender, bio, enabled...
    // (Use Lombok @Data or generate them)
//
    @DynamoDbAttribute("name")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @DynamoDbAttribute("profileImagePath")
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String profileImagePath) { this.profileImagePath = profileImagePath; }

    // DynamoDB doesn't natively support LocalDate. Store as ISO String YYYY-MM-DD.
    @DynamoDbAttribute("dateOfBirth")
    public String getDateOfBirth() {
        return dateOfBirth != null ? dateOfBirth.toString() : null;
    }
    public void setDateOfBirth(String dateOfBirthStr) {
        this.dateOfBirth = dateOfBirthStr != null ? LocalDate.parse(dateOfBirthStr) : null;
    }
     // Getter/Setter for the LocalDate type itself (not mapped to DynamoDB directly without converter)
     @DynamoDbIgnore // Don't map this field directly
     public LocalDate getDateOfBirthAsLocalDate() { return dateOfBirth; }
     public void setDateOfBirthAsLocalDate(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth;}


    @DynamoDbAttribute("gender")
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @DynamoDbAttribute("bio")
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    @DynamoDbAttribute("enabled")
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    // Store timestamps as ISO-8601 strings
    @DynamoDbAttribute("createdAt")
    public String getCreatedAt() {
        return createdAt != null ? createdAt.toString() : null;
    }
    public void setCreatedAt(String createdAtStr) {
        this.createdAt = createdAtStr != null ? LocalDateTime.parse(createdAtStr) : null;
    }

    @DynamoDbAttribute("updatedAt")
    public String getUpdatedAt() {
        return updatedAt != null ? updatedAt.toString() : null;
    }
    public void setUpdatedAt(String updatedAtStr) {
        this.updatedAt = updatedAtStr != null ? LocalDateTime.parse(updatedAtStr) : null;
    }

     // --- Helper Methods for Timestamps (if needed) ---
     @DynamoDbIgnore
     public LocalDateTime getCreatedAtAsLocalDateTime() { return createdAt; }
     public void setCreatedAtAsLocalDateTime(LocalDateTime createdAt) { this.createdAt = createdAt; }

     @DynamoDbIgnore
     public LocalDateTime getUpdatedAtAsLocalDateTime() { return updatedAt; }
     public void setUpdatedAtAsLocalDateTime(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

     @DynamoDbIgnore // Helper to set timestamps on create/update
     public void updateTimestamps() {
         LocalDateTime now = LocalDateTime.now();
         if (this.createdAt == null) {
             this.createdAt = now;
         }
         this.updatedAt = now;
     }

     @DynamoDbIgnore // Helper to generate ID for new users
     public void generateId() {
         if (this.userId == null) {
             this.userId = UUID.randomUUID().toString();
         }
     }

	@Override
	public String toString() {
		return "User [userId=" + userId + ", googleId=" + googleId + ", email=" + email + " ]";
	}
     
     
}