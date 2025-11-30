package com.health.chat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for input validation functionality.
 * Validates Requirements 8.5: Input validation error handling
 */
public class InputValidationTest {

    @Test
    public void testMessageValidation_Valid() {
        String validMessage = "体重65kg、体脂肪率15%";
        InputValidator.ValidationResult result = InputValidator.validateMessage(validMessage);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testMessageValidation_TooLong() {
        String longMessage = "a".repeat(141);
        InputValidator.ValidationResult result = InputValidator.validateMessage(longMessage);
        
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrorMessage().contains("exceeds maximum length"));
    }

    @Test
    public void testMessageValidation_Empty() {
        InputValidator.ValidationResult result = InputValidator.validateMessage("");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be empty"));
    }

    @Test
    public void testMessageValidation_Null() {
        InputValidator.ValidationResult result = InputValidator.validateMessage(null);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
    }

    @Test
    public void testUsernameValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validateUsername("user123");
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testUsernameValidation_TooShort() {
        InputValidator.ValidationResult result = InputValidator.validateUsername("ab");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least 3 characters"));
    }

    @Test
    public void testUsernameValidation_TooLong() {
        String longUsername = "a".repeat(51);
        InputValidator.ValidationResult result = InputValidator.validateUsername(longUsername);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot exceed 50 characters"));
    }

    @Test
    public void testUsernameValidation_InvalidCharacters() {
        InputValidator.ValidationResult result = InputValidator.validateUsername("user@123");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("can only contain"));
    }

    @Test
    public void testPasswordValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validatePassword("password123");
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testPasswordValidation_TooShort() {
        InputValidator.ValidationResult result = InputValidator.validatePassword("pass");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least 8 characters"));
    }

    @Test
    public void testTokenValidation_Valid() {
        String validToken = "header.payload.signature";
        InputValidator.ValidationResult result = InputValidator.validateToken(validToken);
        
        assertTrue(result.isValid());
    }

    @Test
    public void testTokenValidation_Invalid() {
        InputValidator.ValidationResult result = InputValidator.validateToken("invalid-token");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Invalid token format"));
    }

    @Test
    public void testWeightValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validateWeight(65.5);
        
        assertTrue(result.isValid());
    }

    @Test
    public void testWeightValidation_TooLow() {
        InputValidator.ValidationResult result = InputValidator.validateWeight(15.0);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least"));
    }

    @Test
    public void testWeightValidation_TooHigh() {
        InputValidator.ValidationResult result = InputValidator.validateWeight(350.0);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot exceed"));
    }

    @Test
    public void testWeightValidation_Null() {
        InputValidator.ValidationResult result = InputValidator.validateWeight(null);
        
        // Null is valid (optional field)
        assertTrue(result.isValid());
    }

    @Test
    public void testBodyFatValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validateBodyFat(15.5);
        
        assertTrue(result.isValid());
    }

    @Test
    public void testBodyFatValidation_TooLow() {
        InputValidator.ValidationResult result = InputValidator.validateBodyFat(2.0);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("at least"));
    }

    @Test
    public void testBodyFatValidation_TooHigh() {
        InputValidator.ValidationResult result = InputValidator.validateBodyFat(65.0);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot exceed"));
    }

    @Test
    public void testDateValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validateDateString("2024-11-30");
        
        assertTrue(result.isValid());
    }

    @Test
    public void testDateValidation_Invalid() {
        InputValidator.ValidationResult result = InputValidator.validateDateString("11/30/2024");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("ISO format"));
    }

    @Test
    public void testUserIdValidation_Valid() {
        InputValidator.ValidationResult result = InputValidator.validateUserId("user-123");
        
        assertTrue(result.isValid());
    }

    @Test
    public void testUserIdValidation_Empty() {
        InputValidator.ValidationResult result = InputValidator.validateUserId("");
        
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("required"));
    }
}
