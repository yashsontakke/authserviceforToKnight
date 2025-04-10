package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class EmailAddressResponse {
    public PersonMetadata metadata;
    public String value; // The email address
    public String type;
    // getters/setters...
    public String getValue() { return value; }
    public PersonMetadata getMetadata() { return metadata; }
}