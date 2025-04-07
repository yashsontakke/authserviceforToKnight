package com.example.demo.controller;




import org.slf4j.Logger; // Using SLF4J for logging (recommended)
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.model.User;
import com.example.demo.security.UserPrincipal;

@RestController
@RequestMapping("/api/users") // Base path for user-related endpoints
@CrossOrigin(origins = "http://localhost:3000") // Keep CORS configuration
public class UserController {

    // Initialize a logger for this class
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Simplified endpoint to receive and log user profile data including an optional image.
     * --- THIS IS FOR TESTING/LOGGING ONLY ---
     * Lacks authentication, validation, file storage, and database interaction.
     */
    @PostMapping(value = "/createuser", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> receiveAndLogUserProfileData(
            @RequestParam("name") String name,
            @RequestParam("dateOfBirth") String dateOfBirth, // Received as String for now
            @RequestParam("gender") String gender,
            @RequestParam("bio") String bio,
            // Make image optional by setting required = false
            @RequestParam(value = "image", required = false) MultipartFile image) {

        // --- Logging the received data ---
        log.info("--- Received request on /api/users/createuser ---");
        log.info("Name           : {}", name);
        log.info("Date of Birth  : {}", dateOfBirth); // Logged as raw string
        log.info("Gender         : {}", gender);
        log.info("Bio            : {}", bio);

        // Log basic info about the uploaded file, if present
        if (image != null && !image.isEmpty()) {
            log.info("Image File     : Received");
            log.info("  Original Name: {}", image.getOriginalFilename());
            log.info("  Content Type : {}", image.getContentType());
            log.info("  Size (bytes) : {}", image.getSize());
        } else {
            log.info("Image File     : Not provided or empty.");
        }
        log.info("--------------------------------------------------");

        // --- Temporary Success Response ---
        // Send a simple response indicating data was received.
        // Replace this with meaningful response later (e.g., updated User object or status).
        String message = String.format("Received data for user '%s'. Image provided: %b",
                                       name, (image != null && !image.isEmpty()));
        return ResponseEntity.ok(message);
    }
    
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUserProfile(
            // Inject the UserPrincipal object representing the authenticated user.
            // This is populated by JwtAuthenticationFilter via UserDetailsService.
            @AuthenticationPrincipal UserPrincipal currentUser) {

        if (currentUser == null) {
            // This case should ideally be prevented by Spring Security's filter chain
            // if the endpoint is correctly configured as authenticated.
            log.warn("Access attempt to /api/users/me without authenticated principal.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        // Assuming your UserPrincipal class has a way to access the underlying User entity
        // (e.g., a getUser() method you added, or if UserPrincipal IS your User entity)
        // If UserPrincipal *is* the User entity (and User implements UserDetails), you could cast:
        // User user = (User) currentUser;
        // If UserPrincipal *wraps* the User entity (more common):
        User user = currentUser.getUser(); // Assuming you added a getUser() method to UserPrincipal
        log.info("Fetching profile for authenticated user ID: {}", user.getUserId());
        // Return the User object.
        // Consider using a DTO (Data Transfer Object) pattern here if you want to
        // exclude sensitive fields (like password hash if it existed) or format data differently.
        // For now, returning the User entity directly is okay for profile info.
        return ResponseEntity.ok(user);
    }

    // You will add back the proper implementation with authentication,
    // service calls, file saving, and error handling here later.

}