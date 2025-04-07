package com.example.demo.dto;

import java.time.DateTimeException;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDate {
    // Need static logger field if used within methods like toLocalDate
    private static final Logger log = LoggerFactory.getLogger(GoogleDate.class);

    public Integer year; // Can be null if user hides year
    public Integer month;
    public Integer day;
     // Add getters/setters or keep public

    // Helper to convert to LocalDate, handling potential nulls/errors
    public LocalDate toLocalDate() {
        if (year == null || month == null || day == null) {
            log.warn("Incomplete birthday components received: Year={}, Month={}, Day={}", year, month, day);
            return null; // Cannot form a complete LocalDate
        }
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            log.error("Invalid date components received from People API: Y={}, M={}, D={}", year, month, day, e);
            return null; // Invalid date components
        }
    }
}