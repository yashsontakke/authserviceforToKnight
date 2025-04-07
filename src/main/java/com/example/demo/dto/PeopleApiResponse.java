package com.example.demo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeopleApiResponse {
    public List<BirthdayResponse> birthdays;
    // Add getters/setters or keep public
}