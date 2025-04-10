package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BirthdayResponse {
    public PersonMetadata metadata; // Include metadata to potentially find the PROFILE birthday
    public GoogleDate date;
    // getters/setters...
     public GoogleDate getDate() { return date; }
     public PersonMetadata getMetadata() { return metadata; }
}