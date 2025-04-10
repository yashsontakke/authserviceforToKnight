package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PhotoResponse {
     public PersonMetadata metadata;
     public String url;
     @JsonProperty("default") // Maps the "default" JSON field to isDefault field
     public Boolean isDefault; // Use Boolean object type
     // getters/setters...
     public String getUrl() { return url; }
     public Boolean getIsDefault() { return isDefault; } // Getter follows Java convention
     public PersonMetadata getMetadata() { return metadata; }
}