package com.health.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.TankaPoem;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class S3DataRepository implements DataRepository {
    private static final Logger LOGGER = Logger.getLogger(S3DataRepository.class.getName());
    private static final int MAX_RETRIES = 3;
    
    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper;

    public S3DataRepository(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void saveHealthData(String userId, HealthData data) {
        String key = buildHealthDataKey(userId, data.getDate(), data.getTimestamp().toString());
        saveObject(key, data);
    }

    @Override
    public List<HealthData> getHealthDataByDateRange(String userId, LocalDate start, LocalDate end) {
        List<HealthData> results = new ArrayList<>();
        String prefix = String.format("users/%s/health/", userId);
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                try {
                    HealthData data = getObject(s3Object.key(), HealthData.class);
                    if (data != null && data.getDate() != null &&
                        !data.getDate().isBefore(start) && !data.getDate().isAfter(end)) {
                        results.add(data);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve health data from key: " + s3Object.key(), e);
                }
            }
        } catch (S3Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list health data for user: " + userId, e);
            throw new RuntimeException("Failed to retrieve health data", e);
        }
        
        return results;
    }

    @Override
    public void saveNutritionInfo(String userId, LocalDate date, NutritionInfo info) {
        String key = buildNutritionKey(userId, date);
        saveObject(key, info);
    }

    @Override
    public NutritionInfo getNutritionInfo(String userId, LocalDate date) {
        String key = buildNutritionKey(userId, date);
        return getObject(key, NutritionInfo.class);
    }

    @Override
    public void saveMentalState(String userId, LocalDate date, MentalState state) {
        String key = buildMentalStateKey(userId, date);
        saveObject(key, state);
    }

    @Override
    public MentalState getMentalState(String userId, LocalDate date) {
        String key = buildMentalStateKey(userId, date);
        return getObject(key, MentalState.class);
    }

    @Override
    public void saveTanka(String userId, TankaPoem tanka) {
        String key = buildTankaKey(userId, tanka.getDate());
        saveObject(key, tanka);
    }

    @Override
    public List<TankaPoem> getTankaHistory(String userId) {
        List<TankaPoem> results = new ArrayList<>();
        String prefix = String.format("users/%s/tanka/", userId);
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                try {
                    TankaPoem tanka = getObject(s3Object.key(), TankaPoem.class);
                    if (tanka != null) {
                        results.add(tanka);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to retrieve tanka from key: " + s3Object.key(), e);
                }
            }
        } catch (S3Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list tanka history for user: " + userId, e);
            throw new RuntimeException("Failed to retrieve tanka history", e);
        }
        
        return results;
    }

    // Helper methods for building S3 keys with date-based directory structure
    private String buildHealthDataKey(String userId, LocalDate date, String timestamp) {
        return String.format("users/%s/health/%d/%02d/%s.json",
                userId,
                date.getYear(),
                date.getMonthValue(),
                timestamp.replace(":", "-"));
    }

    private String buildNutritionKey(String userId, LocalDate date) {
        return String.format("users/%s/nutrition/%d/%02d/%02d.json",
                userId,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth());
    }

    private String buildMentalStateKey(String userId, LocalDate date) {
        return String.format("users/%s/mental/%d/%02d/%02d.json",
                userId,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth());
    }

    private String buildTankaKey(String userId, LocalDate date) {
        return String.format("users/%s/tanka/%d/%02d/%02d.json",
                userId,
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth());
    }

    private String buildUserProfileKey(String userId) {
        return String.format("users/%s/profile.json", userId);
    }

    @Override
    public void saveUserProfile(com.health.chat.model.UserProfile profile) {
        String key = buildUserProfileKey(profile.getUserId());
        saveObject(key, profile);
    }

    @Override
    public com.health.chat.model.UserProfile getUserProfile(String userId) {
        String key = buildUserProfileKey(userId);
        return getObject(key, com.health.chat.model.UserProfile.class);
    }

    @Override
    public com.health.chat.model.UserProfile getUserProfileByUsername(String username) {
        // List all user profiles and find by username
        String prefix = "users/";
        
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : listResponse.contents()) {
                if (s3Object.key().endsWith("/profile.json")) {
                    try {
                        com.health.chat.model.UserProfile profile = getObject(s3Object.key(), com.health.chat.model.UserProfile.class);
                        if (profile != null && username.equals(profile.getUsername())) {
                            return profile;
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to retrieve user profile from key: " + s3Object.key(), e);
                    }
                }
            }
        } catch (S3Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search for user by username: " + username, e);
            throw new RuntimeException("Failed to search for user", e);
        }
        
        return null;
    }

    // Generic save method with retry logic
    private void saveObject(String key, Object data) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                String jsonContent = objectMapper.writeValueAsString(data);
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/json")
                        .build();
                
                s3Client.putObject(putRequest, RequestBody.fromString(jsonContent));
                LOGGER.info("Successfully saved object to S3: " + key);
                return;
            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.SEVERE, "Failed to serialize object for key: " + key, e);
                throw new RuntimeException("Failed to serialize data", e);
            } catch (S3Exception e) {
                lastException = e;
                attempts++;
                LOGGER.log(Level.WARNING, 
                    String.format("Failed to save to S3 (attempt %d/%d): %s", attempts, MAX_RETRIES, key), e);
                
                if (attempts >= MAX_RETRIES) {
                    break;
                }
                
                try {
                    Thread.sleep(1000 * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        LOGGER.log(Level.SEVERE, "Failed to save to S3 after " + MAX_RETRIES + " attempts: " + key, lastException);
        throw new RuntimeException("Failed to save data to S3", lastException);
    }

    // Generic get method with retry logic
    private <T> T getObject(String key, Class<T> clazz) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                
                byte[] objectBytes = s3Client.getObjectAsBytes(getRequest).asByteArray();
                T result = objectMapper.readValue(objectBytes, clazz);
                LOGGER.info("Successfully retrieved object from S3: " + key);
                return result;
            } catch (NoSuchKeyException e) {
                LOGGER.log(Level.INFO, "Object not found in S3: " + key);
                return null;
            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.SEVERE, "Failed to deserialize object from key: " + key, e);
                throw new RuntimeException("Failed to deserialize data", e);
            } catch (S3Exception e) {
                lastException = e;
                attempts++;
                LOGGER.log(Level.WARNING, 
                    String.format("Failed to retrieve from S3 (attempt %d/%d): %s", attempts, MAX_RETRIES, key), e);
                
                if (attempts >= MAX_RETRIES) {
                    break;
                }
                
                try {
                    Thread.sleep(1000 * attempts); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        LOGGER.log(Level.SEVERE, "Failed to retrieve from S3 after " + MAX_RETRIES + " attempts: " + key, lastException);
        throw new RuntimeException("Failed to retrieve data from S3", lastException);
    }
}
