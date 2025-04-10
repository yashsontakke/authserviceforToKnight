package com.example.demo.dto;
// Add imports for Lombok or generate Getters/Setters/toString manually
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the location update message structure.
 * userId and userDateTime are populated by the backend.
 * latitude and longitude come from the frontend request.
 */
@Data // Lombok: Adds getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Adds no-arg constructor
public class LocationMessage {

    private String userId; // Internal application user ID (String UUID), SET BY BACKEND
    private Double latitude; // Use Double to allow null check from request body
    private Double longitude; // Use Double to allow null check from request body
    private String userDateTime; // IST Formatted String "YYYY-MM-DDTHH:mm:ss+05:30[Asia/Kolkata]", SET BY BACKEND   

}