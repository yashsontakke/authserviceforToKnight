package com.example.demo.service;
import java.io.IOException; // Keep relevant exceptions
import java.security.GeneralSecurityException; // Keep relevant exceptions
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient; // Import WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.demo.dto.BirthdayResponse;
import com.example.demo.dto.EmailAddressResponse;
import com.example.demo.dto.GenderResponse;
import com.example.demo.dto.NameResponse;
import com.example.demo.dto.PeopleApiResponse;
import com.example.demo.dto.PersonMetadata;
import com.example.demo.model.User;
import com.example.demo.repository.UserDynamoDbRepository;

import reactor.core.publisher.Mono; // Import Mono


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
 @Transactional
 public User verifyAndProcessGoogleUser(String accessToken)
         throws GeneralSecurityException, IOException, IllegalArgumentException { // Consider refining exceptions

     log.debug("Attempting to process Google Access Token via People API...");

     // --- Step 1: Call Google People API for combined info ---
     PeopleApiResponse person = getPersonFromGoogle(accessToken);
     if (person == null) {
          throw new IllegalArgumentException("Failed to retrieve valid person data from Google People API.");
     }

     // --- Step 2: Extract required information ---
     String googleId = extractGoogleId(person);
     String email = extractPrimaryField(person.getEmailAddresses(), e -> e.value);
     String name = extractPrimaryField(person.getNames(), n -> n.displayName);
     String pictureUrl = extractPrimaryPhotoUrl(person); // Use specific logic for default photo
     String gender = extractPrimaryField(person.getGenders(), g -> g.formattedValue != null ? g.formattedValue : g.value); // Prefer formatted value
     LocalDate dob = extractBirthday(person);

     if (googleId == null) {
         throw new IllegalArgumentException("Could not determine Google ID (sub) from People API response.");
     }
     log.info("People API data processed for Google ID: {}, Email: {}, Name: {}, Gender: {}, DOB: {}",
              googleId, email, name, gender, dob);

     // --- Step 3: Find or Create user in local DB ---
     final String finalEmail = email; // Need final variable for lambda
     final String finalName = name;
     final String finalPictureUrl = pictureUrl;
     final String finalGender = gender;
     final LocalDate finalDob = dob;

     User user = userDynamoDbRepository.findByGoogleId(googleId)
             .map(existingUser -> {
                 // User exists - update if necessary
                 log.info("Existing user found (ID: {}). Updating details if changed.", existingUser.getUserId());
                 boolean updated = false;
                 if (finalName != null && !finalName.equals(existingUser.getName())) { existingUser.setName(finalName); updated = true; }
                 if (finalEmail != null && !finalEmail.equals(existingUser.getEmail())) { /* careful */ existingUser.setEmail(finalEmail); updated = true; }
                
                 if (pictureUrl != null && !StringUtils.hasText(existingUser.getProfileImagePath())) {
                	 log.info("Existing image path found : ",existingUser.getProfileImagePath());
                     log.debug("Setting initial profile image path for user {} from Google (local path was empty)", existingUser.getUserId());
                     existingUser.setProfileImagePath(pictureUrl); // Use Google pic only if no custom one
                     updated = true;
                } else {
                     log.info("Preserving existing profileImagePath for user {} (likely custom S3 image or existing Google image)", existingUser.getUserId());
                     // Do nothing, keep the existing path (which could be S3 key/URL or old Google URL)
                }
                 
//                 if (finalPictureUrl != null && !finalPictureUrl.equals(existingUser.getProfileImagePath())) { existingUser.setProfileImagePath(finalPictureUrl); updated = true; }
                 if (finalGender != null && !finalGender.equals(existingUser.getGender())) { existingUser.setGender(finalGender); updated = true; }
                 // Compare LocalDate objects
                 if (finalDob != null && !finalDob.equals(existingUser.getDateOfBirthAsLocalDate())) { existingUser.setDateOfBirthAsLocalDate(finalDob); updated = true; }
                 else if (finalDob == null && existingUser.getDateOfBirthAsLocalDate() != null) { existingUser.setDateOfBirthAsLocalDate(null); updated = true; } // Handle removed DOB

                 if (updated) {
                     log.debug("Saving updated user details for Google ID: {}", googleId);
                     userDynamoDbRepository.save(existingUser);
                 }
                 return existingUser;
             })
             .orElseGet(() -> {
                 // User doesn't exist - create a new one
                 log.info("Creating new user for Google ID: {}", googleId);
                 User newUser = new User();
                 newUser.generateId(); // Generate internal UUID userId
                 newUser.setGoogleId(googleId);
                 newUser.setEmail(finalEmail);
                 newUser.setName(finalName);
                 newUser.setProfileImagePath(finalPictureUrl);
                 newUser.setGender(finalGender);
                 newUser.setDateOfBirthAsLocalDate(finalDob);
                 newUser.setEnabled(true);
                 log.debug("Saving new user with Google ID: {}", googleId);
                 userDynamoDbRepository.save(newUser);
                 return newUser;
             });

     log.info("Returning processed user with internal ID: {}, Email: {}", user.getUserId(), user.getEmail());
     return user;
 }
 
//--- Helper to call People API (request multiple fields) ---
 private PeopleApiResponse getPersonFromGoogle(String accessToken) {
     // Request multiple fields in one call
     String personFields = "names,emailAddresses,photos,birthdays,genders,metadata"; // Added metadata for primary/source checks
     String peopleApiUrl = "https://people.googleapis.com/v1/people/me?personFields=" + personFields;
     log.debug("Calling Google People API endpoint: {}", peopleApiUrl);
     try {
         return webClient.get()
                 .uri(peopleApiUrl)
                 .header("Authorization", "Bearer " + accessToken)
                 .retrieve()
                 .onStatus(status -> status.isError(), // Handle 4xx/5xx errors
                           clientResponse -> clientResponse.bodyToMono(String.class)
                               .flatMap(body -> {
                                   log.error("Google People API Error - Status: {}, Body: {}", clientResponse.statusCode(), body);
                                   // Check for 403 specifically, might mean insufficient scope granted by user
                                   if(clientResponse.statusCode() == HttpStatus.FORBIDDEN) {
                                        return Mono.error(new GeneralSecurityException("Permission denied by user or insufficient API scopes for People API. Status: 403"));
                                   }
                                   return Mono.error(new IOException("Error accessing Google People API. Status: " + clientResponse.statusCode()));
                               }))
                 .bodyToMono(PeopleApiResponse.class)
                 .block(); // Synchronous call
     } catch (Exception e) {
          log.error("Failed to call or parse Google People API response", e);
          // Throw a meaningful exception
          throw new RuntimeException("Could not retrieve required person data from Google: " + e.getMessage(), e);
     }
 }

 // --- Helper methods to safely extract primary/best values ---

 private String extractGoogleId(PeopleApiResponse person) {
      // Google ID ('sub') is often in metadata.source.id where source.type == 'ACCOUNT'
      // Check primary email first, then primary name
      if (person.getEmailAddresses() != null) {
         for (EmailAddressResponse email : person.getEmailAddresses()) {
              if (email.getMetadata() != null && Boolean.TRUE.equals(email.getMetadata().getPrimary())
                      && email.getMetadata().getSource() != null && "ACCOUNT".equals(email.getMetadata().getSource().getType())) {
                  return email.getMetadata().getSource().getId();
              }
          }
      }
      // Fallback to checking primary name metadata if not found in email
      if (person.getNames() != null) {
         for (NameResponse name : person.getNames()) {
              if (name.getMetadata() != null && Boolean.TRUE.equals(name.getMetadata().getPrimary())
                      && name.getMetadata().getSource() != null && "ACCOUNT".equals(name.getMetadata().getSource().getType())) {
                  return name.getMetadata().getSource().getId();
              }
          }
      }
      log.error("Could not extract primary ACCOUNT source ID (googleId/sub) from People API response.");
      return null; // Or throw?
  }

 // Generic helper to find primary field value from a list
 private <T> String extractPrimaryField(List<T> list, java.util.function.Function<T, String> valueExtractor) {
      if (list == null || list.isEmpty()) return null;
      return list.stream()
          // Assuming list items have a getMetadata() method returning PersonMetadata
          .filter(item -> {
              try {
                  PersonMetadata meta = (PersonMetadata) item.getClass().getMethod("getMetadata").invoke(item);
                  return meta != null && Boolean.TRUE.equals(meta.getPrimary());
              } catch (Exception e) { return false; } // Handle reflection exceptions/missing method
          })
          .map(valueExtractor)
          .findFirst()
           // Fallback to first item if no primary is marked (or reflection failed)
          .orElseGet(() -> {
              try { return valueExtractor.apply(list.get(0)); }
              catch (Exception e) { return null; }
          });
  }

  private String extractPrimaryPhotoUrl(PeopleApiResponse person) {
      if (person.getPhotos() == null || person.getPhotos().isEmpty()) return null;
      return person.getPhotos().stream()
          .filter(p -> p.getMetadata() != null && Boolean.TRUE.equals(p.getIsDefault())) // Use getIsDefault()
          .map(p -> p.getUrl())
          .findFirst()
          .orElse(person.getPhotos().get(0).getUrl()); // Fallback to first photo
  }

 
   private LocalDate extractBirthday(PeopleApiResponse person) {
      if (person.getBirthdays() == null || person.getBirthdays().isEmpty()) return null;
       return person.getBirthdays().stream()
                 .filter(b -> b.getDate() != null && b.getMetadata() != null && b.getMetadata().getSource() != null && "PROFILE".equals(b.getMetadata().getSource().getType())) // Look for PROFILE source
                 .map(b -> b.getDate().toLocalDate())
                 .filter(java.util.Objects::nonNull) // Filter out nulls from failed conversions
                 .findFirst()
                 .orElseGet(() -> person.getBirthdays().get(0).getDate() != null ? person.getBirthdays().get(0).getDate().toLocalDate() : null); // Fallback
  }

}