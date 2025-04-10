package com.example.demo.service;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserDynamoDbRepository;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDynamoDbRepository userDynamoDbRepository;
    private final S3StorageService s3StorageService;
    private final StringRedisTemplate redisTemplate; // Add Redis dependency
    private final RedisTemplate<String, Object> objectRedisTemplate; // For Sorted Sets (matches)

    
    public UserServiceImpl(UserDynamoDbRepository userDynamoDbRepository, S3StorageService s3StorageService ,  RedisTemplate<String, Object> objectRedisTemplate , StringRedisTemplate redisTemplate) {
        this.userDynamoDbRepository = userDynamoDbRepository;
        this.s3StorageService = s3StorageService;
		this.redisTemplate = redisTemplate;
		this.objectRedisTemplate = objectRedisTemplate;
    }

    @Override
    public User updateUserProfile(String userId, UpdateProfileRequest request) throws UserNotFoundException {
        log.info("Attempting to update profile (bio/image) for user ID: {}", userId);

        // 1. Fetch the existing user
        User user = userDynamoDbRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // 2. Handle Image Upload (if provided)
        MultipartFile imageFile = request.getImageFile();
        String newImageKey = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            log.debug("New profile image provided for user: {}", userId);
            try {
                // Optional: Delete old S3 image
                // String oldImageKey = user.getProfileImagePath();
                // if (oldImageKey != null) { s3StorageService.deleteFile(oldImageKey); }

                newImageKey = s3StorageService.uploadFile(imageFile);
                log.info("New profile image uploaded for user {}. Key: {}", userId, newImageKey);
            } catch (IOException | RuntimeException e) { // Catch potential exceptions from S3 service
                log.error("Failed to upload profile image to S3 for user {}: {}", userId, e.getMessage(), e);
                // Decide how to handle: throw error, or just skip image update?
                // Let's skip image update on failure for now.
                newImageKey = null;
            }
        }

        // 3. Update User Entity Fields (Bio and Image Path only)
        // Allow setting bio to empty string if desired
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        // Update image path *only if* a new image was successfully uploaded
        if (newImageKey != null) {
            user.setProfileImagePath(newImageKey); // Store the S3 object key or URL
        }

        // 4. Save Updated User to DynamoDB
        try {
            // updateTimestamps() should be called within save if defined in repository, or call here
            // user.updateTimestamps(); // If needed here
            userDynamoDbRepository.save(user);
            log.info("Successfully updated profile (bio/image) for user ID: {}", userId);
        } catch (Exception e) {
             log.error("Failed to save updated user profile to DynamoDB for user {}: {}", userId, e.getMessage(), e);
             throw new RuntimeException("Failed to save user profile update.", e); // Re-throw
        }
        
        return user;
    }
    
  //New method to get nearby users with full info
    @Override
    public Set<User> getNearbyUsers(String userId) {
        try {
            // Construct the Redis key pattern
        	log.info("userId in service: {}", userId); // ✅ Placeholder with argument
            String nearbyKeyPattern = "nearby:" + userId + ":*";

            // Fetch all keys matching the pattern
            Set<String> nearbyKeys = redisTemplate.keys(nearbyKeyPattern);
            
            log.info("near by keys :{}" , nearbyKeys);

            // Extract nearby user IDs
            Set<String> nearbyUserIds = new HashSet<>();
            if (nearbyKeys != null) {
                for (String key : nearbyKeys) {
                    String[] parts = key.split(":");
                    if (parts.length == 3) { // Ensure key has correct structure
                        nearbyUserIds.add(parts[2]); // parts[2] is the nearby user ID
                    }
                }
            }
            log.info("near by User Ids :{}" , nearbyUserIds);

            // If no nearby users, return empty set
            if (nearbyUserIds.isEmpty()) {
                return new HashSet<>();
            }

            // Fetch full user details from DynamoDB
//            Set<User> nearbyUsers = new HashSet<>();
//            for (String nearbyUserId : nearbyUserIds) {
//                Optional<User> user = userDynamoDbRepository.findById(nearbyUserId);
//                if (user != null) {
//                    nearbyUsers.add(user);
//                }
//            }
            Set<User> nearbyUsers = new HashSet<>();
            for (String nearbyUserId : nearbyUserIds) {
                userDynamoDbRepository.findById(nearbyUserId)
                    .ifPresent(nearbyUsers::add);
            }
            

            return nearbyUsers;

        } catch (Exception e) {
            // Log the error (uncomment and configure logger if needed)
             log.error("Error fetching nearby users for userId: " + userId, e);
            return new HashSet<>();
        }
    }
    
    @Override
    public List<String> getActiveMatches(String userId) {
        String matchesKey = "matches:" + userId;

        long currentTime = System.currentTimeMillis() / 1000;

        Set<Object> matches = objectRedisTemplate
            .opsForZSet()
            .rangeByScore(matchesKey, currentTime, Double.POSITIVE_INFINITY);

        // Return only active matches
        return matches.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    
    @Override
    public String handleLike(String userId, String likedUserId) {
        // Key for user liking another user
        String likeKey = "nearby:" + userId + ":" + likedUserId;
        String reverseLikeKey = "nearby:" + likedUserId + ":" + userId;

        // Step 1: Record the like with a 1-hour expiration updating expiry 
        redisTemplate.opsForValue().set(likeKey, "true", 1, TimeUnit.HOURS);

        // Step 2: Check if the other user has liked back
        String reverseLike = redisTemplate.opsForValue().get(reverseLikeKey);
        if ("true".equals(reverseLike)) {

            // // Delete both keys
            redisTemplate.delete(Arrays.asList(likeKey, reverseLikeKey));
            // but key will again be generated if came nearby again 

            // Step 3: Mutual like detected, store the match
            long currentTime = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
            long expireTime = currentTime + 24 * 60 * 60; // 24 hours from now

            // Store match for userId
            String matchesKeyUser = "matches:" + userId;
            objectRedisTemplate.opsForZSet().add(matchesKeyUser, likedUserId, expireTime);

            // Store match for likedUserId
            String matchesKeyLiked = "matches:" + likedUserId;
            objectRedisTemplate.opsForZSet().add(matchesKeyLiked, userId, expireTime);

            return "Match created between " + userId + " and " + likedUserId;
        } else {
            // updating expiry if someone has liked you 
            redisTemplate.opsForValue().set(reverseLikeKey, "false", 1, TimeUnit.HOURS);
        }

        // No match yet
        return userId + " liked " + likedUserId + ". Waiting for a match.";
    }

}