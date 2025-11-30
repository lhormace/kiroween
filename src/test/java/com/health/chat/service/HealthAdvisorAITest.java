package com.health.chat.service;

import com.health.chat.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MCPBasedHealthAdvisor implementation.
 */
class HealthAdvisorAITest {
    
    private MCPBasedHealthAdvisor advisor;
    private MockMCPClient mockMCPClient;
    
    @BeforeEach
    void setUp() {
        mockMCPClient = new MockMCPClient();
        advisor = new MCPBasedHealthAdvisor(mockMCPClient);
    }
    
    @Test
    void testGenerateAdviceWithPositiveMentalState() {
        // Arrange
        HealthData data = createHealthData(65.0, 18.0, List.of("サラダ", "鶏肉"), List.of("ジョギング"), null);
        MentalState mentalState = new MentalState(EmotionalTone.POSITIVE, 0.8, List.of("やる気"));
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getMainAdvice());
        assertTrue(result.getMainAdvice().contains("素晴らしい") || 
                   result.getMainAdvice().contains("調子"));
        assertFalse(result.getActionableRecommendations().isEmpty());
    }
    
    @Test
    void testGenerateAdviceWithDiscouragedMentalState() {
        // Arrange
        HealthData data = createHealthData(70.0, null, List.of("ラーメン"), null, null);
        MentalState mentalState = new MentalState(EmotionalTone.DISCOURAGED, 0.3, List.of("疲れた"));
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getMainAdvice());
        assertTrue(result.getMainAdvice().contains("無理せず") || 
                   result.getMainAdvice().contains("できることから"));
        assertFalse(result.getActionableRecommendations().isEmpty());
    }
    
    @Test
    void testGenerateAdviceWithConsultationContent() {
        // Arrange
        HealthData data = createHealthData(68.0, null, null, null, "最近体重が増えて心配です");
        MentalState mentalState = new MentalState(EmotionalTone.NEUTRAL, 0.5, List.of());
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getMainAdvice());
        assertTrue(result.getMainAdvice().contains("ご相談") || 
                   result.getMainAdvice().contains("お気持ち"));
    }
    
    @Test
    void testGenerateAdviceIncludesResearchReferences() {
        // Arrange
        HealthData data = createHealthData(65.0, 18.0, List.of("野菜"), List.of("ウォーキング"), null);
        MentalState mentalState = new MentalState(EmotionalTone.NEUTRAL, 0.5, List.of());
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert
        assertNotNull(result);
        assertNotNull(result.getReferences());
        assertFalse(result.getReferences().isEmpty());
    }
    
    @Test
    void testGenerateAdviceWithMCPException() {
        // Arrange
        mockMCPClient.setShouldThrowException(true);
        HealthData data = createHealthData(65.0, null, List.of("パン"), null, null);
        MentalState mentalState = new MentalState(EmotionalTone.NEUTRAL, 0.5, List.of());
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert - should still generate advice even without MCP
        assertNotNull(result);
        assertNotNull(result.getMainAdvice());
        assertNotNull(result.getActionableRecommendations());
        assertTrue(result.getReferences().isEmpty());
    }
    
    @Test
    void testActionableRecommendationsAreGenerated() {
        // Arrange
        HealthData data = createHealthData(65.0, 18.0, List.of("サラダ"), List.of("ジョギング"), null);
        MentalState mentalState = new MentalState(EmotionalTone.POSITIVE, 0.8, List.of());
        UserProfile profile = createUserProfile();
        
        // Act
        AdviceResult result = advisor.generateAdvice(data, mentalState, profile);
        
        // Assert
        assertNotNull(result.getActionableRecommendations());
        assertFalse(result.getActionableRecommendations().isEmpty());
        // Should have multiple recommendations
        assertTrue(result.getActionableRecommendations().size() >= 1);
    }
    
    // Helper methods
    
    private HealthData createHealthData(Double weight, Double bodyFat, 
                                       List<String> foods, List<String> exercises, 
                                       String comment) {
        return new HealthData(
            "user123",
            LocalDate.now(),
            LocalDateTime.now(),
            weight,
            bodyFat,
            foods,
            exercises,
            comment
        );
    }
    
    private UserProfile createUserProfile() {
        return new UserProfile(
            "user123",
            "testuser",
            "hashedpassword",
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now()
        );
    }
    
    /**
     * Mock MCP Client for testing
     */
    private static class MockMCPClient implements MCPClient {
        private boolean shouldThrowException = false;
        
        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }
        
        @Override
        public List<ResearchReference> fetchNutritionResearch(String query) throws MCPException {
            if (shouldThrowException) {
                throw new MCPException("MCP service unavailable");
            }
            return List.of(new ResearchReference(
                "nutrition",
                "バランスの取れた食事が健康維持に重要です",
                "Nutrition Research Journal"
            ));
        }
        
        @Override
        public List<ResearchReference> fetchSleepResearch(String query) throws MCPException {
            if (shouldThrowException) {
                throw new MCPException("MCP service unavailable");
            }
            return List.of(new ResearchReference(
                "sleep",
                "7-8時間の睡眠が推奨されます",
                "Sleep Science Journal"
            ));
        }
        
        @Override
        public List<ResearchReference> fetchExerciseResearch(String query) throws MCPException {
            if (shouldThrowException) {
                throw new MCPException("MCP service unavailable");
            }
            return List.of(new ResearchReference(
                "exercise",
                "週150分の中強度運動が推奨されます",
                "Exercise Physiology Journal"
            ));
        }
        
        @Override
        public List<ResearchReference> fetchAllResearch(String query) throws MCPException {
            if (shouldThrowException) {
                throw new MCPException("MCP service unavailable");
            }
            List<ResearchReference> all = new ArrayList<>();
            all.addAll(fetchNutritionResearch(query));
            all.addAll(fetchSleepResearch(query));
            all.addAll(fetchExerciseResearch(query));
            return all;
        }
    }
}
