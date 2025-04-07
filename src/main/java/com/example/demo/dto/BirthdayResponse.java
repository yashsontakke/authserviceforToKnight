package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BirthdayResponse {
    public GoogleDate date;
     // Add getters/setters or keep public
}