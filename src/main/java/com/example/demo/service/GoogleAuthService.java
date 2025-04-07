package com.example.demo.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;

import com.example.demo.model.User;

@Service
public interface GoogleAuthService {

	/**
     * Verifies the Google ID token, finds or creates a user in the local database,
     * and returns the persistent User entity.
     *
     * @param idTokenString The Google ID token received from the frontend.
     * @return The persistent User entity (found or newly created).
     * @throws GeneralSecurityException If token verification fails due to security issues.
     * @throws IOException If there's an issue communicating with Google servers.
     * @throws IllegalArgumentException If the token is invalid or required claims are missing.
     */
    User verifyAndProcessGoogleUser(String idTokenString)
            throws GeneralSecurityException, IOException, IllegalArgumentException;

}