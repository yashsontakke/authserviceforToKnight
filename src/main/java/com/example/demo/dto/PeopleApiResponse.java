package com.example.demo.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PeopleApiResponse {
    public List<BirthdayResponse> birthdays;
    public String resourceName;
    public String etag;
    public List<NameResponse> names;
    public List<EmailAddressResponse> emailAddresses;
    public List<PhotoResponse> photos;
    public List<GenderResponse> genders; // Add genders list
    public List<NameResponse> getNames() {return names;} // Example getter
}