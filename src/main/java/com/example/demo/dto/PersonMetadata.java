package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PersonMetadata { // Includes Source for finding Google ID (sub)
    public Source source;
    public Boolean primary;
    // getters/setters...
    public Source getSource() { return source; }
    public Boolean getPrimary() { return primary; }
}