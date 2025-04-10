package com.example.demo.service;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LocationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.geo.GeoHash;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class KafkaPollingService {

    private final KafkaConsumer<String, String> consumer;
    
    private final StringRedisTemplate redisTemplate;
    
    private final UserLocationService userLocationService ;

    public KafkaPollingService(ConsumerFactory<String, String> consumerFactory , StringRedisTemplate redisTemplate , UserLocationService userLocationService) {
        this.consumer = (KafkaConsumer<String, String>) consumerFactory.createConsumer("my-group", "");
        this.redisTemplate = redisTemplate;
		this.userLocationService = userLocationService;       
    }

    @PostConstruct
    public void init() {
        consumer.subscribe(Collections.singletonList("user-locations"));      
    }

    @Scheduled(fixedRate = 10000) // Every 1 minute(60000)
    public void pollMessages() {
    	Map<String, Set<String>> map = userLocationService.findNearbyUsersForAll(5);
        System.out.print(map);
    	System.out.print("polling message .... ");
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) return;
        
     // Temporary buffer to store messages before writing to Redis
        List<LocationMessage> locationMessages = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            LocationMessage locationMessage = parseMessage(record.value());
            locationMessages.add(locationMessage);
        }

        // Batch update Redis after processing all messages
        bulkUpdateRedis(locationMessages);
        
        // Consumer acknowledgment: Happens when the consumer commits the offset (after processing).
        // If a consumer fails before committing the offset, the message is reprocessed when it restarts.  
        consumer.commitSync(); // Commit offsets only if messages were processed                                
    }
    
 // Parse Kafka message into LocationMessage object
    private LocationMessage parseMessage(String jsonMessage) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonMessage, LocationMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Kafka message: " + jsonMessage, e);
        }
    }

    private void bulkUpdateRedis(List<LocationMessage> locationMessages) {
        if (locationMessages.isEmpty()) return;

        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        for (LocationMessage message : locationMessages) {
        	        	
        	 // Parse user’s IST time
            ZonedDateTime userTime = ZonedDateTime.parse(message.getUserDateTime());
            
            // Ensure user time is in IST (+05:30)
            if (!userTime.getZone().equals(ZoneId.of("Asia/Kolkata"))) {
            	System.out.print("Error: User time must be in IST (+05:30)");
            }
 
            // Your current IST time
            ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.of("Asia/Kolkata"));                      

            // Calculate difference in seconds (now - userTime)
            long secondsDifference = ChronoUnit.SECONDS.between(userTime, now);
            long oneHourInSeconds = 3600;

            // Check if user’s time is not older than 1 hour 
            if (secondsDifference > oneHourInSeconds || !userTime.isBefore(now)) {                          
            	System.out.print("time is wrong ");
            	continue ;
            }
            
            String timeBucket = get5MinTimeBucket(userTime.toString());
        
            String geoPrefix = getGeohashPrefix(message.getLatitude(), message.getLongitude());
            String redisKey = "user_locations:" + geoPrefix+":" + timeBucket;

 
            String dateTimeStr = message.getUserDateTime();
            
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr);

            // Extract hour and minute
            int hour = zonedDateTime.getHour();
            int minute = zonedDateTime.getMinute();
           
            // 🔥 Unique ID for each location entry (Prevents Overwriting)
            String locationKey = message.getUserId() + ":" + hour+":"+minute;
            
         // ✅ Check before adding
            boolean isNewKey = !redisTemplate.hasKey(redisKey);
            
           
            Long result = geoOps.add(redisKey, new Point(message.getLongitude(), message.getLatitude()), locationKey);
            System.out.println("GeoAdd result: " + result);    // If result == 1, a new entry was added.

            if (isNewKey) {
            	setRedisKeyWithExpiration(timeBucket , redisKey);              
            }            
        }
    }
    
    private void setRedisKeyWithExpiration(String timeBucket, String redisKey) {

        try {
            // Parse the timeBucket to get a Date object
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date bucketTime = sdf.parse(timeBucket);
            
            // Create calendar and set it to the bucket time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(bucketTime);
            
            // Set today's date components (year, month, day)
            Calendar today = Calendar.getInstance();
            calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
            
            // Add 1 hour to the bucket time to get expiration time
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            
            // Calculate TTL (time to live) in seconds
            long expirationTimeSeconds = calendar.getTimeInMillis() / 1000;
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            long ttlSeconds = expirationTimeSeconds - currentTimeSeconds;
            
            // Ensure the TTL is positive
            if (ttlSeconds <= 0) {
                // Handle edge case: if time bucket was late in the day and now it's past expiration
                ttlSeconds = 3600; // Default to 1 hour if calculation gives non-positive value
            }
            
            // Set the Redis key expiration
            redisTemplate.expire(redisKey, Duration.ofSeconds(ttlSeconds));
        } catch (ParseException e) {
            // Handle parsing exception
            throw new IllegalArgumentException("Invalid time bucket format: " + timeBucket, e);
        }
    }

    public static String get5MinTimeBucket(String isoDateTimeString) {
        // Parse the ISO-8601 string to ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(isoDateTimeString);
        
        // Get the hour and minute
        int hour = zonedDateTime.getHour();
        int minute = zonedDateTime.getMinute();
        
        // Calculate the nearest 5-minute interval (floor)
        int roundedMinute = (minute / 5) * 5;
        
        // Create a new time with the rounded minute and zeroed seconds/nanos
        ZonedDateTime roundedTime = zonedDateTime
            .withMinute(roundedMinute)
            .withSecond(0)
            .withNano(0);
        
        // Format the time as HH:mm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return formatter.format(roundedTime);
    }
 
    private String getGeohashPrefix(double latitude, double longitude) {
    	return GeoHash.encodeHash(latitude, longitude, 5); // 5 characters precision
    }      
   
    @PreDestroy
    public void shutdown() {
        consumer.close();
    }
}