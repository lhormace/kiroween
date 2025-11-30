package com.health.chat.service;

import com.health.chat.model.DailyNutrition;
import com.health.chat.model.HealthData;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.PFCBalance;
import com.health.chat.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NutritionEstimatorTest {
    
    private DataRepository mockRepository;
    private BasicNutritionEstimator estimator;
    
    @BeforeEach
    void setUp() {
        mockRepository = mock(DataRepository.class);
        estimator = new BasicNutritionEstimator(mockRepository);
    }
    
    @Test
    void testEstimateNutrition_withKnownFood() {
        List<String> foodItems = Arrays.asList("ご飯", "卵");
        
        NutritionInfo result = estimator.estimateNutrition(foodItems);
        
        assertNotNull(result);
        assertTrue(result.getCalories() > 0);
        assertTrue(result.getProtein() > 0);
        assertTrue(result.getFat() > 0);
        assertTrue(result.getCarbohydrate() > 0);
    }
    
    @Test
    void testEstimateNutrition_withEmptyList() {
        List<String> foodItems = Collections.emptyList();
        
        NutritionInfo result = estimator.estimateNutrition(foodItems);
        
        assertNotNull(result);
        assertEquals(0, result.getCalories());
        assertEquals(0, result.getProtein());
        assertEquals(0, result.getFat());
        assertEquals(0, result.getCarbohydrate());
    }
    
    @Test
    void testEstimateNutrition_withUnknownFood() {
        List<String> foodItems = Arrays.asList("unknown_food_xyz");
        
        NutritionInfo result = estimator.estimateNutrition(foodItems);
        
        assertNotNull(result);
        // Should use default estimation
        assertTrue(result.getCalories() > 0);
    }
    
    @Test
    void testCalculateDailyTotal() {
        String userId = "user123";
        LocalDate date = LocalDate.now();
        
        HealthData healthData1 = new HealthData();
        healthData1.setUserId(userId);
        healthData1.setDate(date);
        healthData1.setTimestamp(LocalDateTime.now());
        healthData1.setFoodItems(Arrays.asList("ご飯", "卵"));
        
        HealthData healthData2 = new HealthData();
        healthData2.setUserId(userId);
        healthData2.setDate(date);
        healthData2.setTimestamp(LocalDateTime.now().plusHours(3));
        healthData2.setFoodItems(Arrays.asList("鶏肉"));
        
        when(mockRepository.getHealthDataByDateRange(userId, date, date))
            .thenReturn(Arrays.asList(healthData1, healthData2));
        
        DailyNutrition result = estimator.calculateDailyTotal(userId, date);
        
        assertNotNull(result);
        assertTrue(result.getTotalCalories() > 0);
        assertNotNull(result.getPfcBalance());
    }
    
    @Test
    void testPFCBalanceCalculation() {
        String userId = "user123";
        LocalDate date = LocalDate.now();
        
        HealthData healthData = new HealthData();
        healthData.setUserId(userId);
        healthData.setDate(date);
        healthData.setTimestamp(LocalDateTime.now());
        healthData.setFoodItems(Arrays.asList("ご飯"));
        
        when(mockRepository.getHealthDataByDateRange(userId, date, date))
            .thenReturn(Arrays.asList(healthData));
        
        DailyNutrition result = estimator.calculateDailyTotal(userId, date);
        
        assertNotNull(result.getPfcBalance());
        PFCBalance balance = result.getPfcBalance();
        
        // PFC ratios should sum to approximately 1.0
        double sum = balance.getProteinRatio() + balance.getFatRatio() + balance.getCarbohydrateRatio();
        assertEquals(1.0, sum, 0.01);
        
        // All ratios should be between 0 and 1
        assertTrue(balance.getProteinRatio() >= 0 && balance.getProteinRatio() <= 1);
        assertTrue(balance.getFatRatio() >= 0 && balance.getFatRatio() <= 1);
        assertTrue(balance.getCarbohydrateRatio() >= 0 && balance.getCarbohydrateRatio() <= 1);
    }
    
    @Test
    void testCalculateDailyTotal_withNoFoodData() {
        String userId = "user123";
        LocalDate date = LocalDate.now();
        
        when(mockRepository.getHealthDataByDateRange(userId, date, date))
            .thenReturn(Collections.emptyList());
        
        DailyNutrition result = estimator.calculateDailyTotal(userId, date);
        
        assertNotNull(result);
        assertEquals(0, result.getTotalCalories());
        assertNotNull(result.getPfcBalance());
        assertEquals(0, result.getPfcBalance().getProteinRatio());
        assertEquals(0, result.getPfcBalance().getFatRatio());
        assertEquals(0, result.getPfcBalance().getCarbohydrateRatio());
    }
}
