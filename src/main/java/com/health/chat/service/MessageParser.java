package com.health.chat.service;

import com.health.chat.model.HealthData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MessageParser extracts health-related information from user messages.
 * Supports extraction of weight, body fat percentage, food items, and exercises.
 */
public class MessageParser {
    
    // Weight patterns: "体重65kg", "65キロ", "weight: 65", "65.5kg"
    // Must have either weight keyword OR kg/kilo unit
    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
        "(?:体重|weight)\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kg|キロ|ｋｇ|キログラム)?|" +
        "(\\d+(?:\\.\\d+)?)\\s*(?:kg|キロ|ｋｇ|キログラム)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Body fat patterns: "体脂肪率15%", "15パーセント", "body fat: 15", "15.5%"
    // Must have either body fat keyword OR % unit
    private static final Pattern BODY_FAT_PATTERN = Pattern.compile(
        "(?:体脂肪率|体脂肪|body\\s*fat)\\s*:?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:%|パーセント|％|percent)?|" +
        "(\\d+(?:\\.\\d+)?)\\s*(?:%|パーセント|％)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Food keywords
    private static final String[] FOOD_KEYWORDS = {
        "食べた", "食事", "朝食", "昼食", "夕食", "breakfast", "lunch", "dinner",
        "ate", "eat", "食", "ご飯", "パン", "肉", "魚", "野菜", "果物"
    };
    
    // Exercise keywords
    private static final String[] EXERCISE_KEYWORDS = {
        "運動", "走った", "歩いた", "ジム", "exercise", "run", "walk", "gym",
        "トレーニング", "training", "ランニング", "ウォーキング", "筋トレ"
    };
    
    private static final int MAX_MESSAGE_LENGTH = 140;
    
    /**
     * Validates that the message does not exceed the maximum length.
     * 
     * @param message the message to validate
     * @return true if the message is valid (within length limit), false otherwise
     */
    public boolean validateMessageLength(String message) {
        if (message == null) {
            return false;
        }
        return message.length() <= MAX_MESSAGE_LENGTH;
    }
    
    /**
     * Parses a message and extracts health data.
     * 
     * @param userId the user ID
     * @param message the message to parse
     * @return HealthData object with extracted information
     * @throws IllegalArgumentException if message exceeds maximum length
     */
    public HealthData parseMessage(String userId, String message) {
        if (!validateMessageLength(message)) {
            throw new IllegalArgumentException("Message exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");
        }
        
        HealthData healthData = new HealthData();
        healthData.setUserId(userId);
        healthData.setDate(LocalDate.now());
        healthData.setTimestamp(LocalDateTime.now());
        healthData.setFreeComment(message);
        
        // Extract weight
        Double weight = extractWeight(message);
        healthData.setWeight(weight);
        
        // Extract body fat percentage
        Double bodyFat = extractBodyFat(message);
        healthData.setBodyFatPercentage(bodyFat);
        
        // Extract food items
        List<String> foodItems = extractFoodItems(message);
        healthData.setFoodItems(foodItems);
        
        // Extract exercises
        List<String> exercises = extractExercises(message);
        healthData.setExercises(exercises);
        
        return healthData;
    }
    
    /**
     * Extracts weight value from the message.
     * 
     * @param message the message to parse
     * @return the extracted weight value, or null if not found
     */
    public Double extractWeight(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        
        Matcher matcher = WEIGHT_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                // Try group 1 first (with keyword), then group 2 (with unit only)
                String weightStr = matcher.group(1);
                if (weightStr == null) {
                    weightStr = matcher.group(2);
                }
                if (weightStr != null) {
                    return Double.parseDouble(weightStr);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Extracts body fat percentage from the message.
     * 
     * @param message the message to parse
     * @return the extracted body fat percentage, or null if not found
     */
    public Double extractBodyFat(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        
        Matcher matcher = BODY_FAT_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                // Try group 1 first (with keyword), then group 2 (with % only)
                String bodyFatStr = matcher.group(1);
                if (bodyFatStr == null) {
                    bodyFatStr = matcher.group(2);
                }
                if (bodyFatStr != null) {
                    return Double.parseDouble(bodyFatStr);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Extracts food items from the message.
     * 
     * @param message the message to parse
     * @return list of food items mentioned, or empty list if none found
     */
    public List<String> extractFoodItems(String message) {
        List<String> foodItems = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return foodItems;
        }
        
        String lowerMessage = message.toLowerCase();
        for (String keyword : FOOD_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                // Extract the portion of text around the keyword
                int index = lowerMessage.indexOf(keyword.toLowerCase());
                int start = Math.max(0, index - 10);
                int end = Math.min(message.length(), index + keyword.length() + 20);
                String context = message.substring(start, end).trim();
                if (!foodItems.contains(context)) {
                    foodItems.add(context);
                }
                break; // Only add one food context per message for simplicity
            }
        }
        
        return foodItems;
    }
    
    /**
     * Extracts exercise information from the message.
     * 
     * @param message the message to parse
     * @return list of exercises mentioned, or empty list if none found
     */
    public List<String> extractExercises(String message) {
        List<String> exercises = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return exercises;
        }
        
        String lowerMessage = message.toLowerCase();
        for (String keyword : EXERCISE_KEYWORDS) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                // Extract the portion of text around the keyword
                int index = lowerMessage.indexOf(keyword.toLowerCase());
                int start = Math.max(0, index - 10);
                int end = Math.min(message.length(), index + keyword.length() + 20);
                String context = message.substring(start, end).trim();
                if (!exercises.contains(context)) {
                    exercises.add(context);
                }
                break; // Only add one exercise context per message for simplicity
            }
        }
        
        return exercises;
    }
}
