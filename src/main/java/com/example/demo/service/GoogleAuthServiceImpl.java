package com.example.demo.service;
import java.io.IOException; // Keep relevant exceptions
import java.security.GeneralSecurityException; // Keep relevant exceptions
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient; // Import WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.model.User;
import com.example.demo.repository.UserDynamoDbRepository;

import reactor.core.publisher.Mono; // Import Mono

import com.example.demo.dto.*;


@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

 private static final Logger log = LoggerFactory.getLogger(GoogleAuthServiceImpl.class);

 private final UserDynamoDbRepository userDynamoDbRepository;
 private final WebClient webClient; // Use WebClient for HTTP calls

 // Remove GoogleIdTokenVerifier related fields/constructor params if any


 public GoogleAuthServiceImpl(UserDynamoDbRepository userDynamoDbRepository,
                              WebClient.Builder webClientBuilder) { // Inject Builder
     this.userDynamoDbRepository = userDynamoDbRepository;
     // Configure a WebClient instance (can customize base URLs, headers etc. if needed)
     this.webClient = webClientBuilder.build();
 }


 @Override
 // Method now accepts accessToken instead of idTokenString
 public User verifyAndProcessGoogleUser(String accessToken)
         throws GeneralSecurityException, IOException, IllegalArgumentException {

     log.debug("Attempting to process Google Access Token...");

     // --- Step 1: Verify Access Token & Get User Info ---
     GoogleUserInfo userInfo = getUserInfoFromGoogle(accessToken);
     if (userInfo == null || userInfo.sub == null) {
         throw new IllegalArgumentException("Failed to retrieve valid user info from Google using access token.");
     }

     String googleId = userInfo.sub;
     String email = userInfo.email;
     String name = userInfo.name;
     String pictureUrl = userInfo.picture;
     log.info("Google Access Token validated via UserInfo for Google ID: {}, Email: {}", googleId, email);

     // --- Step 2: Get Birthday info using the same Access Token ---
     LocalDate dob = getBirthdayFromGoogle(accessToken); // Can return null if not found/permitted

     // --- Step 3: Find or Create user in local DB ---
     User user = userDynamoDbRepository.findByGoogleId(googleId)
             .map(existingUser -> {
                 log.info("Existing user found with Google ID: {}. Updating details.", googleId);
                 boolean updated = false;
                 // Update fields based on fresh data from Google
                 if (name != null && !name.equals(existingUser.getName())) {
                     existingUser.setName(name); updated = true;
                 }
                 if (pictureUrl != null && !pictureUrl.equals(existingUser.getProfileImagePath())) {
                      existingUser.setProfileImagePath(pictureUrl); updated = true;
                 }
                 if (email != null && !email.equals(existingUser.getEmail())) {
                      log.warn("User email changed in Google profile for Google ID {}. Updating locally.", googleId);
                      existingUser.setEmail(email); updated = true;
                 }
                 if (dob != null && !dob.equals(existingUser.getDateOfBirthAsLocalDate())) {
                      log.info("Updating DOB for user {}", googleId);
                      existingUser.setDateOfBirthAsLocalDate(dob); updated = true;
                 } else if (dob == null && existingUser.getDateOfBirthAsLocalDate() != null) {
                      // Handle case where birthday was previously set but now removed/hidden in Google
                      log.info("Clearing DOB for user {} as it's no longer available from Google.", googleId);
                      existingUser.setDateOfBirthAsLocalDate(null); updated = true;
                 }

                 if (updated) {
                     log.debug("Saving updated user details for Google ID: {}", googleId);
                     userDynamoDbRepository.save(existingUser); // Save if updated
                 }
                 return existingUser;
             })
             .orElseGet(() -> {
                 // User doesn't exist - create a new one
                 log.info("Creating new user for Google ID: {}", googleId);
                 User newUser = new User();
                 newUser.generateId(); // Generate internal UUID userId
                 newUser.setGoogleId(googleId);
                 newUser.setEmail(email); // Assume email is required for new users
                 newUser.setName(name);
                 newUser.setProfileImagePath(pictureUrl);
                 newUser.setEnabled(true); // Enable by default
                 
                 if (dob != null) {
                	 System.out.print(dob);
                      newUser.setDateOfBirthAsLocalDate(dob);
                 }
                 log.debug("Saving new user with Google ID: {}", googleId);
                 userDynamoDbRepository.save(newUser); // Save the new user
                 return newUser;
             });

     log.info("Returning processed user with internal ID: {}, Email: {}", user.getUserId(), user.getEmail());
     return user; // Return the persistent User entity from your DB
 }


 // --- Helper to call Google UserInfo endpoint ---
 private GoogleUserInfo getUserInfoFromGoogle(String accessToken) {
     String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
     log.debug("Calling Google UserInfo endpoint...");
     try {
         return webClient.get()
                 .uri(userInfoUrl)
                 .header("Authorization", "Bearer " + accessToken)
                 .retrieve()
                 // Handle specific HTTP errors from Google
                 .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                           clientResponse -> clientResponse.bodyToMono(String.class)
                               .flatMap(body -> {
                                   log.error("Google UserInfo Error - Status: {}, Body: {}", clientResponse.statusCode(), body);
                                   // Map to a specific exception indicating token is likely invalid
                                   return Mono.error(new IllegalArgumentException("Invalid Access Token or API error contacting Google UserInfo. Status: " + clientResponse.statusCode()));
                               }))
                 .bodyToMono(GoogleUserInfo.class)
                 .block(); // Use block() for synchronous execution in this service method context
     } catch (WebClientResponseException e) {
          // Catch specific webclient errors if needed, but onStatus handles most HTTP errors
          log.error("WebClient error calling Google UserInfo: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
          throw new IllegalArgumentException("Failed to verify access token with Google UserInfo: " + e.getMessage(), e);
     } catch (Exception e) {
          log.error("Unexpected error calling Google UserInfo endpoint", e);
          throw new RuntimeException("Could not retrieve user info from Google: " + e.getMessage(), e);
     }
 }


 // --- Helper to call Google People API for Birthday ---
 private LocalDate getBirthdayFromGoogle(String accessToken) {
     String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=birthdays";
     log.debug("Calling Google People API endpoint for birthdays...");
     try {
        PeopleApiResponse response = webClient.get()
             .uri(peopleApiUrl)
             .header("Authorization", "Bearer " + accessToken)
             .retrieve()
              // Handle specific errors
             .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                       clientResponse -> clientResponse.bodyToMono(String.class)
                           .flatMap(body -> {
                               // 403 might mean user didn't grant birthday scope or API not enabled
                               log.error("Google People API Error - Status: {}, Body: {}", clientResponse.statusCode(), body);
                               // Don't throw fatal error, just return Mono.empty() or Mono.error with specific type
                               // Returning Mono.empty() will result in null response body downstream
                               // Don't throw IllegalArgumentException here, as token might be valid but scope denied
                               return Mono.error(new IOException("Failed to retrieve birthday from Google People API. Status: " + clientResponse.statusCode()));

                           }))
             .bodyToMono(PeopleApiResponse.class)
             .block(); // Synchronous call

        if (response != null && response.birthdays != null && !response.birthdays.isEmpty()) {
             BirthdayResponse birthdayInfo = response.birthdays.get(0); // Get the primary birthday
             if (birthdayInfo.date != null) {
                 LocalDate dob = birthdayInfo.date.toLocalDate();
                 if(dob != null) {
                      log.info("Birthday data found and parsed: {}", dob);
                      return dob;
                 } else {
                      log.warn("Birthday data found but components were invalid: Year={}, Month={}, Day={}",
                                birthdayInfo.date.year, birthdayInfo.date.month, birthdayInfo.date.day);
                 }
             } else {
                 log.info("Birthday field present in People API response but 'date' component is null.");
             }
        }
        log.info("No valid/complete birthday information found or accessible in People API response.");
        return null; // No birthday found, no permission, or incomplete data

     } catch (WebClientResponseException e) {
         log.error("WebClient error calling Google People API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
         // Don't fail the whole login just because birthday fetch failed. Log and return null.
         return null;
     } catch (Exception e) {
         log.error("Unexpected error calling Google People API endpoint", e);
         // Don't fail the whole login. Log and return null.
         return null;
     }
 }
}