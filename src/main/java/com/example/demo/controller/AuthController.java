package com.example.demo.controller;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration; // For Max-Age

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.http.HttpHeaders; // Import HttpHeaders
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie; // Import ResponseCookie
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
// Removed Transactional here, keep it on the Service implementation
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.GoogleLoginRequest;
import com.example.demo.model.User;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.service.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse; // Needed for adding header directly (alternative)

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Ensure allowCredentials = true
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GoogleAuthService googleAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    // Inject cookie properties
    @Value("${app.jwt.cookie-name}")
    private String jwtCookieName;

    @Value("${app.jwt.cookie-max-age-seconds}")
    private long jwtCookieMaxAgeSeconds;

    // Constructor
    public AuthController(GoogleAuthService googleAuthService,
                          JwtTokenProvider jwtTokenProvider) {
        this.googleAuthService = googleAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    
    // --- Modified /google Endpoint ---
    @PostMapping("/google")
    // Return only User now, token is in cookie
    public ResponseEntity<User> authenticateGoogle(@RequestBody GoogleLoginRequest requestBody, HttpServletResponse httpServletResponse) {
        String accessToken = requestBody.getAccessToken();
        log.info("AuthController: /google request with Access Token: {}", (StringUtils.hasText(accessToken) ? "present" : "null or empty"));

        if (!StringUtils.hasText(accessToken)) {
             return ResponseEntity.badRequest().build();
        }

        try {
            // Step 1: Verify Google Token & process user in DB (via Service)
            User user = googleAuthService.verifyAndProcessGoogleUser(accessToken);

            // Step 2: Generate Your Application's JWT
            String jwt = jwtTokenProvider.generateToken(user);

            // Step 3: Create HttpOnly Cookie
            ResponseCookie cookie = ResponseCookie.from(jwtCookieName, jwt)
                    .httpOnly(true)       // Essential: Prevents JS access
                    .secure(true)         // Essential: Send only over HTTPS (Requires HTTPS setup)
                                          // Set secure(false) ONLY for local HTTP testing if needed, NEVER in prod
                    .path("/")            // Cookie accessible for all paths
                    .maxAge(Duration.ofSeconds(jwtCookieMaxAgeSeconds)) // Set expiration
                    .sameSite("Lax")      // Good default CSRF mitigation (use "Strict" if applicable)
                    // .domain("yourdomain.com") // Set if needed for subdomains
                    .build();

            // Step 4: Add Cookie to Response Header
            httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            log.info("AuthController: Set HttpOnly cookie '{}' for user ID: {}", jwtCookieName, user.getUserId());

            // Step 5: Return User data in response body (Frontend needs this)
            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException | GeneralSecurityException | IOException e) {
            log.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
             log.error("Internal error during authentication: {}", e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- NEW: Logout Endpoint ---
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletResponse httpServletResponse) {
        log.info("AuthController: Processing /logout request");
        // Create a cookie with the same name, empty value, and maxAge=0 to clear it
        ResponseCookie deleteCookie = ResponseCookie.from(jwtCookieName, "")
                .httpOnly(true)
                .secure(true) // Must match original cookie settings
                .path("/")
                .maxAge(0)   // Expire immediately
                .sameSite("Lax") // Must match original cookie settings
                // .domain("yourdomain.com") // Must match original cookie settings if used
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        log.info("AuthController: Cleared HttpOnly cookie '{}'", jwtCookieName);
        return ResponseEntity.ok("Logout successful"); // Or ResponseEntity.noContent()
    }
}