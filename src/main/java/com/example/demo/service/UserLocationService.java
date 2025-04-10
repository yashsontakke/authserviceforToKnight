package com.example.demo.service;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoLocation;
import org.springframework.stereotype.Service;

import com.github.davidmoten.geo.GeoHash;

@Service
public class UserLocationService {

    private final RedisTemplate<String, String> redisTemplate;

    public UserLocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ✅ Fetch All Active Time Buckets (Only Non-Expired Keys)
    private Set<String> getActiveTimeBuckets() {
        return redisTemplate.keys("user_locations:*");  // Fetch only active Redis keys
    }
    public Set<GeoLocation<String>> getAllLocations(String redisKey) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo(); // ✅ Get GeoOperations

        // Fetch all members (user IDs with location info) from the given key
        Set<String> members = redisTemplate.opsForZSet().range(redisKey, 0, -1);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        Set<GeoLocation<String>> locations = new HashSet<>();

        for (String member : members) {
            // ✅ Fetch the geographic position for each user ID
            List<Point> positions = geoOps.position(redisKey, member);
            if (positions != null && !positions.isEmpty()) {
                locations.add(new GeoLocation<>(member, positions.get(0))); // Store user with location
            }
        }

        return locations;
    }
    
    // ✅ Find Nearby Users for All Users (Avoiding Duplicates)
    public Map<String, Set<String>> findNearbyUsersForAll(double searchRadiusKm) {
    	
        Set<String> activeKeys = getActiveTimeBuckets();  // Step 1: Fetch all active buckets
        System.out.print(activeKeys);
        Map<String, Set<Point>> userLocations = new HashMap<>();  // Store user locations
        Map<String, Set<String>> nearbyUsersMap = new HashMap<>();  // Store nearby users
       
        // Step 2: Fetch all users and store locations
        for (String redisKey : activeKeys) {
            for (GeoLocation<String> location : getAllLocations(redisKey)) {
                String fullUserId = location.getName();
                Point point = location.getPoint();
                userLocations.computeIfAbsent(fullUserId, k -> new HashSet<>()).add(point);
            }
        }
        
        for (Map.Entry<String, Set<Point>> entry : userLocations.entrySet()) {
            String userIdWithTimestamp = entry.getKey();
            String baseUserId = userIdWithTimestamp.split(":")[0]; 
            String baseUserTimestamp = userIdWithTimestamp.split(":")[1]+":"+userIdWithTimestamp.split(":")[2]; // Format: HH:mm            
            
            Set<Point> userPoints = entry.getValue();
            Set<String> nearbyUsers = new HashSet<>();
         // For tracking new nearby users to notify about
            Set<String> newNearbyUsers = new HashSet<>();

            for (Point userPoint : userPoints) {
                // Get geohash of the current user
                String userGeohash = getGeohash(userPoint);

                // Get all neighboring geohashes
                Set<String> geohashRegions = getNeighboringGeohashes(userGeohash);
                geohashRegions.add(userGeohash); // Include current geohash                
            
                for (String geohashRegion : geohashRegions) {
                    // Fetch users from this geohash region
                	Set<String> regionUsers = getUsersInGeohashRegion(geohashRegion);                	
                	
                    if (regionUsers == null) continue; // Skip if no users in this region
   
                    for (String otherUserIdWithTimestamp : regionUsers) {
                    	
                        String baseOtherUserId = otherUserIdWithTimestamp.split(":")[0];
                        String otherUserTimestamp = otherUserIdWithTimestamp.split(":")[1]+":"+otherUserIdWithTimestamp.split(":")[2];
                        
//                   *********** // here i have to make an extra check whether they are already matched users or ignored one ********* 
                        if (!baseUserId.equals(baseOtherUserId)) { 
                            Set<Point> otherUserPoints = userLocations.get(otherUserIdWithTimestamp);
                            if (otherUserPoints == null) continue;

                            for (Point otherPoint : otherUserPoints) {
                                if (calculateDistance(userPoint, otherPoint) <= searchRadiusKm) {
                                    nearbyUsers.add(baseOtherUserId);                            
                                    
                                    // Check if this is a new nearby user
                                    String nearbyKey = "nearby:" + baseUserId + ":" + baseOtherUserId;
//                                                      
                                    Boolean isNew = redisTemplate.opsForValue().setIfAbsent(nearbyKey, "1");
                                    
                                    // If we successfully set the key, it's a new nearby user
                                    if (Boolean.TRUE.equals(isNew)) {
                                        newNearbyUsers.add(baseOtherUserId);
                                        long expirySeconds = calculateExpiryTime(baseUserTimestamp, otherUserTimestamp);
                                        
//                                        System.out.println(baseUserTimestamp+" "+otherUserTimestamp);
                                        // Set expiration for the nearby relationship (1 day)
                                        redisTemplate.expire(nearbyKey, Duration.ofSeconds(expirySeconds));
                                    }
                                }
                            }
                        }
                    }
                }
                
            }
            nearbyUsersMap.put(baseUserId, nearbyUsers);
            
            // Send notifications for new nearby users
            if (!newNearbyUsers.isEmpty()) {
                sendNearbyUserNotifications(baseUserId, newNearbyUsers);
            }
        }

        return nearbyUsersMap; 
    }
    
    private void sendNearbyUserNotifications(String userId, Set<String> newNearbyUsers) {
//    	System.out.print(userId + " ");
//    	System.out.println(newNearbyUsers);
    	
    	int size = newNearbyUsers.size();
    	sendPushNotification(userId,"near by found :"+size);
    }

    private void sendPushNotification(String userId, String message) {
        // Implement your push notification service here
        // This could integrate with Firebase Cloud Messaging, OneSignal, etc.
        
        // Example implementation:
        try {
            // Log notification for debugging
//            System.out.println("Sending notification to user " + userId + ": " + message);
            
            // TODO: Replace with your actual notification service
            // pushNotificationService.sendNotification(userId, message);
        } catch (Exception e) {
            // Log error but don't fail the whole process
            System.err.println("Failed to send notification to user " + userId + ": " + e.getMessage());
        }
    }

    private long calculateExpiryTime(String userTimestamp, String otherUserTimestamp) {
        try {
            // Parse the timestamps
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
  
            Date userTime = sdf.parse(userTimestamp);
            Date otherUserTime = sdf.parse(otherUserTimestamp);
            
            
            
            // Get current date components
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            
            // Set up calendars for both timestamps with today's date
            Calendar userCal = Calendar.getInstance();
            userCal.setTime(userTime);
            userCal.set(year, month, day);
            
            Calendar otherUserCal = Calendar.getInstance();
            otherUserCal.setTime(otherUserTime);
            otherUserCal.set(year, month, day);
            
            // Add 1 hour to each timestamp
            userCal.add(Calendar.HOUR_OF_DAY, 1);
            otherUserCal.add(Calendar.HOUR_OF_DAY, 1);
            
            // Get the later of the two expiry times
            long userExpiry = userCal.getTimeInMillis();
            long otherUserExpiry = otherUserCal.getTimeInMillis();
            long laterExpiry = Math.min(userExpiry, otherUserExpiry);
            
            // Calculate seconds from now until expiry
            long currentTimeMillis = System.currentTimeMillis();
            long secondsUntilExpiry = (laterExpiry - currentTimeMillis ) / 1000;
            
            // Ensure minimum expiry of 15 minutes
            return secondsUntilExpiry + 900;
        } catch (ParseException e) {
            // Default expiry if there's an issue parsing timestamps
            return 3600; // 1 hour
        }
    }

    public Set<String> getNearbyUsersForUser(String userId) {
        String nearbyListKey = "nearby:list:" + userId;
        Set<String> nearbyUsers = redisTemplate.opsForSet().members(nearbyListKey);
        return nearbyUsers != null ? nearbyUsers : new HashSet<>();
    }
    
	public Set<String> getUsersInGeohashRegion(String geohashRegion) {
        Set<String> allUsers = new HashSet<>();

        // Get all matching keys for the given geohash region
        Set<String> matchingKeys = redisTemplate.keys("user_locations:" + geohashRegion + ":*");

        if (matchingKeys != null) {
            for (String redisKey : matchingKeys) {
                // Get users from each bucketed key
                Set<String> users = redisTemplate.opsForZSet().range(redisKey, 0, -1);
                if (users != null) {
                    allUsers.addAll(users);
                }
            }
        }

        return allUsers;
    }

    public Set<String> getNeighboringGeohashes(String geohash) {
        List<String> neighbors = GeoHash.neighbours(geohash); // Get 8 neighboring geohashes
        Set<String> geohashRegions = new HashSet<>(neighbors);
        geohashRegions.add(geohash); // Include the original geohash
        return geohashRegions;
    }
    
    public String getGeohash(Point point) {
        return GeoHash.encodeHash(point.getY(), point.getX(), 5); // 5-character precision
    }
    
    // ✅ Calculate Distance Between Two Coordinates (Haversine Formula)
    private double calculateDistance(Point p1, Point p2) {
        double lat1 = p1.getX(), lon1 = p1.getY();
        double lat2 = p2.getX(), lon2 = p2.getY();
        final int R = 6371; // Earth's radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
}
