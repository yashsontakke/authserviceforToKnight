package com.example.demo.controller;


import java.io.IOException;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.GoogleLoginRequest;
import com.example.demo.model.User;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.service.JwtTokenProvider;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

 private static final Logger log = LoggerFactory.getLogger(AuthController.class);

 // --- Inject Services ---
 private final GoogleAuthService googleAuthService; // Use the interface
 private final JwtTokenProvider jwtTokenProvider;
 // No longer need UserRepository here

 public AuthController(GoogleAuthService googleAuthService,
                       JwtTokenProvider jwtTokenProvider) { // Remove UserRepository from constructor
     this.googleAuthService = googleAuthService;
     this.jwtTokenProvider = jwtTokenProvider;
 }

 // --- Simplified Method ---
 @PostMapping("/google")
 // Change @RequestBody type to expect the Access Token DTO
 public ResponseEntity<AuthResponse> authenticateGoogle(@RequestBody GoogleLoginRequest requestBody) {
     // Extract the Access Token
     String accessToken = requestBody.getAccessToken();
     log.info("AuthController received /google request with Access Token: {}", (StringUtils.hasText(accessToken) ? "present" : "null or empty"));

     // Validate the received access token string
     if (!StringUtils.hasText(accessToken)) {
          log.warn("Received null or blank Access Token in /google request body.");
          // Consider returning a structured error response instead of null
          return ResponseEntity.badRequest().build();
     }

     try {
         // --- CRITICAL BACKEND CHANGE NEEDED ---
         // Step 1: Call the service method, passing the ACCESS TOKEN.
         // The service method 'verifyAndProcessGoogleUser' (or a renamed version)
         // MUST NOW handle this Access Token:
         //   1. Verify the Access Token with Google (e.g., call tokeninfo/userinfo endpoint).
         //   2. Extract user info (Google ID, email, name etc.) from the verification response.
         //   3. Use the SAME Access Token to call Google People API for birthday.
         //   4. Perform the "Find or Create" logic in your DB (DynamoDB).
         //   5. Return the persistent User object.
         User user = googleAuthService.verifyAndProcessGoogleUser(accessToken); // Pass Access Token

         // If service call succeeds and returns the User object from your DB...

         // Step 2: Generate Your Application's JWT (remains the same logic)
         String jwt = jwtTokenProvider.generateToken(user);
         log.info("Generated JWT for processed user ID: {}, Email: {}", user.getUserId(), user.getEmail());

         // Step 3: Create Response Body (remains the same logic)
         AuthResponse authResponse = new AuthResponse(jwt, user);

         // Step 4: Return OK response (remains the same logic)
         return ResponseEntity.ok(authResponse);

     // --- Adjust Exception Handling if Service throws different types ---
     } catch (IllegalArgumentException | GeneralSecurityException | IOException e) {
         // These exceptions might still be relevant if thrown by the underlying Google client libraries
         // or your verification logic for the access token. IllegalArgumentException is good for bad tokens.
         log.error("Authentication failed during Google Access Token processing: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
     } catch (Exception e) {
          // Catch broader unexpected exceptions (e.g., database, People API call failures)
          log.error("Unexpected internal error during Google authentication via Access Token: {}", e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
     }
 }
 
}