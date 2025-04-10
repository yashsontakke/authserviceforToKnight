package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GenderResponse { // Added DTO for Gender
    public PersonMetadata metadata;
    public String value; // e.g., "male", "female", "unspecified"
    public String formattedValue; // e.g., "Male", "Female", "Unspecified"
    // getters/setters...
     public String getValue() { return value; }
     public String getFormattedValue() { return formattedValue; }
     public PersonMetadata getMetadata() { return metadata; }
}
