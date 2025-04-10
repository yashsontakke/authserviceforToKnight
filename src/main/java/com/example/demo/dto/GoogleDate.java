package com.example.demo.dto;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GoogleDate {
    private static final Logger log = LoggerFactory.getLogger(GoogleDate.class);
    public Integer year;
    public Integer month;
    public Integer day;
    // getters/setters...
    public Integer getYear() { return year; } // Example getter
    // ... other getters/setters ...

    public LocalDate toLocalDate() {
        if (year == null || month == null || day == null) {
             log.warn("Cannot form LocalDate from incomplete components: Y={}, M={}, D={}", year, month, day);
            return null;
        }
        try { return LocalDate.of(year, month, day); }
        catch (DateTimeException e) {
            log.error("Invalid date components received from People API: Y={}, M={}, D={}", year, month, day, e);
            return null;
        }
    }
}