package com.health.chat.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Input validation utility for the health chat application.
 * Provides comprehensive validation for user inputs.
 */
public class InputValidator {
    
    private static final int MAX_MESSAGE_LENGTH = 140;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;
    
    // Reasonable health data ranges
    private static final double MIN_WEIGHT_KG = 20.0;
    private static final double MAX_WEIGHT_KG = 300.0;
    private static final double MIN_BODY_FAT_PERCENT = 3.0;
    private static final double MAX_BODY_FAT_PERCENT = 60.0;
    
    /**
     * Validation result containing success status and error messages
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
    
    /**
     * Validates a chat message
     */
    public static ValidationResult validateMessage(String message) {
        List<String> errors = new ArrayList<>();
        
        if (message == null) {
            errors.add("Message cannot be null");
            return new ValidationResult(false, errors);
        }
        
        if (message.trim().isEmpty()) {
            errors.add("Message cannot be empty");
        }
        
        if (message.length() > MAX_MESSAGE_LENGTH) {
            errors.add(String.format("Message exceeds maximum length of %d characters (current: %d)", 
                MAX_MESSAGE_LENGTH, message.length()));
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates username
     */
    public static ValidationResult validateUsername(String username) {
        List<String> errors = new ArrayList<>();
        
        if (username == null || username.trim().isEmpty()) {
            errors.add("Username is required");
            return new ValidationResult(false, errors);
        }
        
        if (username.length() < MIN_USERNAME_LENGTH) {
            errors.add(String.format("Username must be at least %d characters", MIN_USERNAME_LENGTH));
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            errors.add(String.format("Username cannot exceed %d characters", MAX_USERNAME_LENGTH));
        }
        
        // Check for valid characters (alphanumeric, underscore, hyphen)
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            errors.add("Username can only contain letters, numbers, underscores, and hyphens");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates password
     */
    public static ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return new ValidationResult(false, errors);
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add(String.format("Password must be at least %d characters", MIN_PASSWORD_LENGTH));
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            errors.add(String.format("Password cannot exceed %d characters", MAX_PASSWORD_LENGTH));
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates authentication token
     */
    public static ValidationResult validateToken(String token) {
        List<String> errors = new ArrayList<>();
        
        if (token == null || token.trim().isEmpty()) {
            errors.add("Authentication token is required");
            return new ValidationResult(false, errors);
        }
        
        // Basic JWT format check (three parts separated by dots)
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            errors.add("Invalid token format");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates weight value
     */
    public static ValidationResult validateWeight(Double weight) {
        List<String> errors = new ArrayList<>();
        
        if (weight == null) {
            // Weight is optional, so null is valid
            return new ValidationResult(true, errors);
        }
        
        if (weight < MIN_WEIGHT_KG) {
            errors.add(String.format("Weight must be at least %.1f kg", MIN_WEIGHT_KG));
        }
        
        if (weight > MAX_WEIGHT_KG) {
            errors.add(String.format("Weight cannot exceed %.1f kg", MAX_WEIGHT_KG));
        }
        
        if (weight.isNaN() || weight.isInfinite()) {
            errors.add("Weight must be a valid number");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates body fat percentage
     */
    public static ValidationResult validateBodyFat(Double bodyFat) {
        List<String> errors = new ArrayList<>();
        
        if (bodyFat == null) {
            // Body fat is optional, so null is valid
            return new ValidationResult(true, errors);
        }
        
        if (bodyFat < MIN_BODY_FAT_PERCENT) {
            errors.add(String.format("Body fat percentage must be at least %.1f%%", MIN_BODY_FAT_PERCENT));
        }
        
        if (bodyFat > MAX_BODY_FAT_PERCENT) {
            errors.add(String.format("Body fat percentage cannot exceed %.1f%%", MAX_BODY_FAT_PERCENT));
        }
        
        if (bodyFat.isNaN() || bodyFat.isInfinite()) {
            errors.add("Body fat percentage must be a valid number");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates a date string in ISO format (YYYY-MM-DD)
     */
    public static ValidationResult validateDateString(String dateStr) {
        List<String> errors = new ArrayList<>();
        
        if (dateStr == null || dateStr.trim().isEmpty()) {
            errors.add("Date is required");
            return new ValidationResult(false, errors);
        }
        
        // Check ISO date format
        if (!dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            errors.add("Date must be in ISO format (YYYY-MM-DD)");
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * Validates user ID
     */
    public static ValidationResult validateUserId(String userId) {
        List<String> errors = new ArrayList<>();
        
        if (userId == null || userId.trim().isEmpty()) {
            errors.add("User ID is required");
            return new ValidationResult(false, errors);
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
