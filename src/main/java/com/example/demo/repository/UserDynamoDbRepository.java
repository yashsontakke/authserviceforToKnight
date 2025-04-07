package com.example.demo.repository;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

@Repository // Register as a Spring bean
public class UserDynamoDbRepository {

    private static final Logger log = LoggerFactory.getLogger(UserDynamoDbRepository.class);
    private final DynamoDbTable<User> userTable;
    // Define GSI names as constants
    private static final String GOOGLE_ID_INDEX = "googleId-index";
    private static final String EMAIL_INDEX = "email-index";
    private final DynamoDbClient client;


    public UserDynamoDbRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient client) {
        this.userTable = enhancedClient.table("users", TableSchema.fromBean(User.class));
        this.client = client;
    }

    public void save(User user) {
        log.debug("Saving user with ID: {}", user.getUserId());
        System.out.print(user);
//        user.updateTimestamps(); // Update timestamps before saving
        userTable.putItem(user); // Use putItem for both create and update
        log.info("Successfully saved user with ID: {}", user.getUserId());
    }

    public Optional<User> findById(String userId) {
        log.debug("Finding user by ID: {}", userId);
        User user = userTable.getItem(Key.builder().partitionValue(userId).build());
        return Optional.ofNullable(user);
    }

    public Optional<User> findByGoogleId(String googleId) {
        log.debug("Finding user by Google ID using index '{}': {}", GOOGLE_ID_INDEX, googleId);
        try {
            DynamoDbIndex<User> index = userTable.index(GOOGLE_ID_INDEX);
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(googleId).build());
            var results = index.query(queryConditional);

            ListTablesResponse tables = client.listTables();
            System.out.println("Tables: " + tables.tableNames());
            // Assuming googleId is unique, we expect 0 or 1 item
            // Query returns pages, get items from the first page
            var users = results.stream().flatMap(page -> page.items().stream()).collect(Collectors.toList());

            if (users.isEmpty()) {
                 log.debug("No user found for Google ID: {}", googleId);
                return Optional.empty();
            } else if (users.size() > 1) {
                 log.warn("Multiple users found for Google ID: {}! Returning the first one.", googleId);
                 // This indicates a potential data integrity issue if googleId should be unique
            }
             log.debug("User found for Google ID: {}", googleId);
            return Optional.of(users.get(0));

        } catch (Exception e) {
            log.error("Error querying {} for googleId {}: {}", GOOGLE_ID_INDEX, googleId, e.getMessage(), e);
             // Depending on the exception, you might re-throw or return empty
             return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
         log.debug("Finding user by Email using index '{}': {}", EMAIL_INDEX, email);
         try {
             DynamoDbIndex<User> index = userTable.index(EMAIL_INDEX);
             QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build());
             var results = index.query(queryConditional);
             var users = results.stream().flatMap(page -> page.items().stream()).collect(Collectors.toList());

             if (users.isEmpty()) {
                 return Optional.empty();
             } else if (users.size() > 1) {
                 log.warn("Multiple users found for Email: {}! Returning the first one.", email);
             }
             return Optional.of(users.get(0));
         } catch (Exception e) {
             log.error("Error querying {} for email {}: {}", EMAIL_INDEX, email, e.getMessage(), e);
             return Optional.empty();
         }
     }

    // Add delete method if needed: userTable.deleteItem(...)
}