package com.example.demo.dto;

public class GoogleLoginRequest {
	private String accessToken;

    // Default constructor is required for JSON deserialization
    public GoogleLoginRequest() {}

    // Getter and Setter
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
