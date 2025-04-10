package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserInfo {
    public String sub; // This is the unique Google ID
    public String email;
    public Boolean email_verified;
    public String gender;
    public String name;
    public String picture;
    public String given_name;
    public String family_name;
    // Add getters/setters or keep public for simplicity if inner class
}
