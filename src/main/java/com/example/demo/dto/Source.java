package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Source {
    public String type; // e.g., "ACCOUNT", "PROFILE", "CONTACT"
    public String id; // The unique Google ID (sub) is often here when type="ACCOUNT"
    // getters/setters...
    public String getType() { return type; }
    public String getId() { return id; }
}