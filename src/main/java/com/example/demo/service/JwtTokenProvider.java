package com.example.demo.service;


import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger; // Use Logger
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.model.User;

// Import specific exceptions
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // Import specific exception

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-ms}") long validityInMilliseconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secret); // Assuming Base64 encoded secret
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInMilliseconds;
    }

    // --- Existing generateToken method ---
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityInMilliseconds);
        String subject = user.getUserId() != null ? String.valueOf(user.getUserId()) : user.getEmail();
        if (subject == null) {
             throw new IllegalArgumentException("Cannot create JWT without user identifier (ID or Email)");
        }

        log.debug("Generating JWT for subject: {}, Expiration: {}", subject, expiryDate);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    // --- NEW: Method to get user identifier (subject) from token ---
    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key) // Verify using the secret key
                .build()
                .parseSignedClaims(token) // Parse the token
                .getPayload(); // Get the payload (claims)

        return claims.getSubject(); // Return the 'sub' claim
    }

    // --- NEW: Method to validate the token ---
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            log.debug("JWT Token is valid");
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}