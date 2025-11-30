package com.health.chat.service;

import com.health.chat.model.HealthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageParserTest {
    
    private MessageParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new MessageParser();
    }
    
    @Test
    void testValidateMessageLength_ValidMessage() {
        String validMessage = "体重65kg 体脂肪率15% 朝食にパンを食べた";
        assertTrue(parser.validateMessageLength(validMessage));
    }
    
    @Test
    void testValidateMessageLength_ExactlyMaxLength() {
        String message = "a".repeat(140);
        assertTrue(parser.validateMessageLength(message));
    }
    
    @Test
    void testValidateMessageLength_TooLong() {
        String tooLongMessage = "a".repeat(141);
        assertFalse(parser.validateMessageLength(tooLongMessage));
    }
    
    @Test
    void testValidateMessageLength_NullMessage() {
        assertFalse(parser.validateMessageLength(null));
    }
    
    @Test
    void testExtractWeight_JapaneseFormat() {
        String message = "今日の体重は65kgです";
        Double weight = parser.extractWeight(message);
        assertEquals(65.0, weight);
    }
    
    @Test
    void testExtractWeight_DecimalValue() {
        String message = "体重65.5キロ";
        Double weight = parser.extractWeight(message);
        assertEquals(65.5, weight);
    }
    
    @Test
    void testExtractWeight_EnglishFormat() {
        String message = "weight: 70kg";
        Double weight = parser.extractWeight(message);
        assertEquals(70.0, weight);
    }
    
    @Test
    void testExtractWeight_NoWeight() {
        String message = "今日は元気です";
        Double weight = parser.extractWeight(message);
        assertNull(weight);
    }
    
    @Test
    void testExtractBodyFat_JapaneseFormat() {
        String message = "体脂肪率15%";
        Double bodyFat = parser.extractBodyFat(message);
        assertEquals(15.0, bodyFat);
    }
    
    @Test
    void testExtractBodyFat_DecimalValue() {
        String message = "体脂肪率15.5パーセント";
        Double bodyFat = parser.extractBodyFat(message);
        assertEquals(15.5, bodyFat);
    }
    
    @Test
    void testExtractBodyFat_NoBodyFat() {
        String message = "今日は元気です";
        Double bodyFat = parser.extractBodyFat(message);
        assertNull(bodyFat);
    }
    
    @Test
    void testExtractFoodItems_Found() {
        String message = "朝食にパンを食べた";
        var foodItems = parser.extractFoodItems(message);
        assertFalse(foodItems.isEmpty());
    }
    
    @Test
    void testExtractFoodItems_NotFound() {
        String message = "今日は元気です";
        var foodItems = parser.extractFoodItems(message);
        assertTrue(foodItems.isEmpty());
    }
    
    @Test
    void testExtractExercises_Found() {
        String message = "今日は5km走った";
        var exercises = parser.extractExercises(message);
        assertFalse(exercises.isEmpty());
    }
    
    @Test
    void testExtractExercises_NotFound() {
        String message = "今日は元気です";
        var exercises = parser.extractExercises(message);
        assertTrue(exercises.isEmpty());
    }
    
    @Test
    void testParseMessage_Complete() {
        String message = "体重65kg 体脂肪率15% 朝食にパンを食べた ジムで運動した";
        HealthData data = parser.parseMessage("user123", message);
        
        assertNotNull(data);
        assertEquals("user123", data.getUserId());
        assertEquals(65.0, data.getWeight());
        assertEquals(15.0, data.getBodyFatPercentage());
        assertFalse(data.getFoodItems().isEmpty());
        assertFalse(data.getExercises().isEmpty());
        assertEquals(message, data.getFreeComment());
    }
    
    @Test
    void testParseMessage_TooLong() {
        String tooLongMessage = "a".repeat(141);
        assertThrows(IllegalArgumentException.class, () -> {
            parser.parseMessage("user123", tooLongMessage);
        });
    }
}
