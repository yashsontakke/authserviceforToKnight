package com.example.demo.exception;



import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception to indicate that a requested resource was not found.
 * Often results in an HTTP 404 Not Found status when handled globally.
 */


/**
 * Custom exception to indicate that a requested User entity was not found.
 * Results in an HTTP 404 Not Found status.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Keep this for automatic 404 mapping
public class UserNotFoundException extends RuntimeException { // Rename class

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}