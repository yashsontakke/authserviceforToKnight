package com.example.demo.service;

import java.util.List;
import java.util.Set;

import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.model.User;

import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public interface UserService {

    /**
     * Updates the profile (specifically bio and profile image) for the user identified by userId.
     * Handles image upload to S3 if a new image is provided.
     *
     * @param userId The ID (UUID String) of the user whose profile needs updating.
     * @param request DTO containing the updated bio and potentially an image file.
     * @return The updated User object after saving to the database.
     * @throws ResourceNotFoundException if the user with userId is not found.
     */
    User updateUserProfile(String userId, UpdateProfileRequest request) throws ResourceNotFoundException;

	Set<User> getNearbyUsers(String userId);

	String handleLike(String userId, String likedUserId);

	List<String> getActiveMatches(String userId);

    // Other methods...
}