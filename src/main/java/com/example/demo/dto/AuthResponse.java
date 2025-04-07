package com.example.demo.dto;

import com.example.demo.model.User;

public class AuthResponse {
    private String token;
    private User user;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    // --- Getters ---
    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

     // Optional: Setters if needed, but typically immutable is fine for response DTOs
    // public void setToken(String token) { this.token = token; }
    // public void setUser(User user) { this.user = user; }
}