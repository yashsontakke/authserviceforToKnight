package com.example.demo.controller;


import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LocationMessage;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.LocationKafkaProducer;

@RestController
@RequestMapping("/api/location")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class LocationController {

 private static final Logger log = LoggerFactory.getLogger(LocationController.class);
 private static final ZoneId INDIA_ZONE_ID = ZoneId.of("Asia/Kolkata");
 private static final DateTimeFormatter IST_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

 // --- Inject the Kafka Producer Service ---
 private final LocationKafkaProducer locationKafkaProducer;
 // ---------------------------------------

 // --- Updated Constructor ---
 @Autowired
 public LocationController(LocationKafkaProducer locationKafkaProducer) { // Inject via constructor
     this.locationKafkaProducer = locationKafkaProducer;
     // Inject LocationService for saving to DB if needed
     // this.locationService = locationService;
 }
 // ---------------------------

 @PostMapping("/update")
 public ResponseEntity<String> updateLocation(
         @AuthenticationPrincipal UserPrincipal currentUser,
         @RequestBody LocationMessage locationData) {

     if (currentUser == null) { /* ... unauthorized ... */ }
     String userId = currentUser.getId(); // Get user ID

     if (locationData == null || locationData.getLatitude() == null || locationData.getLongitude() == null) {
          /* ... bad request ... */ }

     // Populate backend fields
     locationData.setUserId(userId);
     long epochMilli = System.currentTimeMillis();
     Instant instant = Instant.ofEpochMilli(epochMilli);
     ZonedDateTime zonedDateTimeIST = ZonedDateTime.ofInstant(instant, INDIA_ZONE_ID);
     String userDateTimeIST = zonedDateTimeIST.format(IST_FORMATTER);
     locationData.setUserDateTime(userDateTimeIST);

     log.info("Processed LocationMessage: {}", locationData);

     // --- Send message to Kafka ---
     try {
         locationKafkaProducer.sendMessage(locationData);
         log.info("Location message queued to Kafka for user {}", userId);
     } catch (Exception e) {
         // Log if the send initiation itself fails (less common)
         log.error("Error initiating send to Kafka for user {}: {}", userId, e.getMessage());
         // Depending on requirements, maybe return an internal server error?
         // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to queue location update.");
         // Or just proceed, as the producer logs async errors separately
     }
     // -----------------------------

     // --- TODO: Persist location to DynamoDB (optional, as discussed) ---
     // locationService.saveLocation(locationData);
     // ------------------------------------------

     // Update response message
     return ResponseEntity.ok("Location received and queued."); // Updated message
 }
}