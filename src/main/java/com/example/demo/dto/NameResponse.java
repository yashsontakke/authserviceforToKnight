package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NameResponse {
    public PersonMetadata metadata;
    public String displayName;
    public String familyName;
    public String givenName;
    // getters/setters...
    public String getDisplayName() { return displayName; }
    public PersonMetadata getMetadata() { return metadata; }
}