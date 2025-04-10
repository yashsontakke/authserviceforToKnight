package com.example.demo.controller;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UpdateProfileRequest;
import com.example.demo.model.User;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Keep CORS
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    // Remove FileStorageService injection if only UserService calls it

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @SuppressWarnings("unused")
	@GetMapping("/nearby")
    public ResponseEntity<Set<User>> getNearbyUsersForAuthenticatedUser(@AuthenticationPrincipal UserPrincipal currentUser) {
    	 String userId = currentUser.getUser().getUserId(); 


      	

        Set<User> nearbyUsers = userService.getNearbyUsers(userId);

        if (nearbyUsers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(nearbyUsers);
    }
    
    @PostMapping("/like/{likedUserId}")
    public ResponseEntity<String> likeUser(@PathVariable String likedUserId, @AuthenticationPrincipal UserPrincipal currentUser) {
    	 String userId = currentUser.getId(); 

        String response = userService.handleLike(userId, likedUserId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/matches")
    public ResponseEntity<List<String>> getMatches(@AuthenticationPrincipal UserPrincipal currentUser) {
    	 String userId = currentUser.getId(); 
        List<String> activeMatches = userService.getActiveMatches(userId);
        return ResponseEntity.ok(activeMatches);
    }

	 

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUserProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request // <-- Inject HttpServletRequest
            ) {

        // --- Add this block to ensure CSRF token generation/loading ---
        // Accessing the token attribute often triggers CookieCsrfTokenRepository
        // to load/generate the token and make it available for the response cookie.
        // "_csrf" is the default attribute name where Spring Security stores the token.
//        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        // Alternative using default name:
        // CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");

//        if (csrfToken != null) {
             // You don't strictly need to *do* anything with the token here,
             // just accessing it is often enough. Logging is useful for debugging.
//             log.debug("/me endpoint: CSRF Token [{}] = {}", csrfToken.getHeaderName(), csrfToken.getToken());
//        } else {
             // This might happen on the very first request before login, which is fine.
             // But it *should* be non-null for authenticated requests if CSRF is configured correctly.
//             log.warn("/me endpoint: Could not find CsrfToken in request attributes.");
//        }
        // -----------------------------------------------------------

        // Proceed with existing logic
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = currentUser.getUser(); // Assuming UserPrincipal has getUser()
        log.info("Fetching profile for authenticated user ID: {}", user.getUserId());
        return ResponseEntity.ok(user);
    }

//    @GetMapping("/me")
//    public ResponseEntity<User> getCurrentUserProfile(
//            @AuthenticationPrincipal UserPrincipal currentUser,
//            CsrfToken csrfToken // 👈 this forces CSRF token generation
//    ) {
//        if (currentUser == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        User user = currentUser.getUser();
//        log.info("Fetching profile for user ID: {}", user.getUserId());
//
//        // Optional: log the token just for debugging
//        log.debug("CSRF token generated: {}", csrfToken.getToken());
//
//        return ResponseEntity.ok(user);
//    }

    // --- Updated Profile Update Endpoint ---
    @PatchMapping(value = "/me/profile", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            // Use @RequestPart which works well for multipart/form-data
            @RequestPart(value = "bio", required = false) String bio, // Bio might be optional
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile // Image is optional
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }
        String userId = currentUser.getId(); 
            
         // If your UserPrincipal directly holds the String UUID:
         // String userId = currentUser.getIdAsString(); // Example


        log.info("Received profile update request for user ID: {}", userId);

        try {
            // Create the request DTO
            UpdateProfileRequest updateRequest = new UpdateProfileRequest();
            updateRequest.setBio(bio); // Will be null if not sent
            updateRequest.setImageFile(imageFile); // Will be null if not sent

            // Call the service to handle the update logic
            User updatedUser = userService.updateUserProfile(userId, updateRequest);

            // Return appropriate response DTO based on updatedUser
            return ResponseEntity.ok(updatedUser); // Or a simpler success message/DTO

        } catch (ResourceNotFoundException e) {
            log.warn("User not found during profile update for ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update profile for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Profile update failed.");
        }
    }
}