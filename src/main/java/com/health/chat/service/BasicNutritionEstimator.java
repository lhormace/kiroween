package com.health.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.chat.model.DailyNutrition;
import com.health.chat.model.HealthData;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.PFCBalance;
import com.health.chat.repository.DataRepository;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BasicNutritionEstimator implements NutritionEstimator {
    private static final Logger LOGGER = Logger.getLogger(BasicNutritionEstimator.class.getName());
    
    private final Map<String, FoodItem> foodDatabase;
    private final DataRepository dataRepository;
    
    public BasicNutritionEstimator(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
        this.foodDatabase = loadFoodDatabase();
    }
    
    @Override
    public NutritionInfo estimateNutrition(List<String> foodItems) {
        if (foodItems == null || foodItems.isEmpty()) {
            return new NutritionInfo(0, 0, 0, 0);
        }
        
        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbohydrate = 0;
        
        for (String foodText : foodItems) {
            FoodItem food = findFoodInDatabase(foodText);
            if (food != null) {
                totalCalories += food.calories;
                totalProtein += food.protein;
                totalFat += food.fat;
                totalCarbohydrate += food.carbohydrate;
                LOGGER.info("Matched food: " + food.name + " for input: " + foodText);
            } else {
                LOGGER.warning("Could not find nutrition data for: " + foodText);
                // Use default estimation for unknown foods
                totalCalories += 100;
                totalProtein += 3;
                totalFat += 3;
                totalCarbohydrate += 15;
            }
        }
        
        return new NutritionInfo(totalCalories, totalProtein, totalFat, totalCarbohydrate);
    }
    
    @Override
    public DailyNutrition calculateDailyTotal(String userId, LocalDate date) {
        // Get all health data for the specified date
        List<HealthData> healthDataList = dataRepository.getHealthDataByDateRange(userId, date, date);
        
        double totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbohydrate = 0;
        
        // Aggregate nutrition from all food entries for the day
        for (HealthData healthData : healthDataList) {
            if (healthData.getFoodItems() != null && !healthData.getFoodItems().isEmpty()) {
                NutritionInfo nutrition = estimateNutrition(healthData.getFoodItems());
                totalCalories += nutrition.getCalories();
                totalProtein += nutrition.getProtein();
                totalFat += nutrition.getFat();
                totalCarbohydrate += nutrition.getCarbohydrate();
            }
        }
        
        // Calculate PFC balance
        PFCBalance pfcBalance = calculatePFCBalance(totalProtein, totalFat, totalCarbohydrate);
        
        return new DailyNutrition(totalCalories, pfcBalance);
    }
    
    private PFCBalance calculatePFCBalance(double protein, double fat, double carbohydrate) {
        // Convert grams to calories: protein=4kcal/g, fat=9kcal/g, carb=4kcal/g
        double proteinCalories = protein * 4;
        double fatCalories = fat * 9;
        double carbCalories = carbohydrate * 4;
        
        double totalCalories = proteinCalories + fatCalories + carbCalories;
        
        if (totalCalories == 0) {
            return new PFCBalance(0, 0, 0);
        }
        
        double proteinRatio = proteinCalories / totalCalories;
        double fatRatio = fatCalories / totalCalories;
        double carbohydrateRatio = carbCalories / totalCalories;
        
        return new PFCBalance(proteinRatio, fatRatio, carbohydrateRatio);
    }
    
    private FoodItem findFoodInDatabase(String foodText) {
        if (foodText == null || foodText.trim().isEmpty()) {
            return null;
        }
        
        String normalizedInput = foodText.toLowerCase().trim();
        
        // First try exact match on name
        for (FoodItem food : foodDatabase.values()) {
            if (food.name.equalsIgnoreCase(normalizedInput)) {
                return food;
            }
        }
        
        // Then try aliases
        for (FoodItem food : foodDatabase.values()) {
            for (String alias : food.aliases) {
                if (normalizedInput.contains(alias.toLowerCase())) {
                    return food;
                }
            }
        }
        
        // Finally try partial match on name
        for (FoodItem food : foodDatabase.values()) {
            if (normalizedInput.contains(food.name.toLowerCase()) || 
                food.name.toLowerCase().contains(normalizedInput)) {
                return food;
            }
        }
        
        return null;
    }
    
    private Map<String, FoodItem> loadFoodDatabase() {
        Map<String, FoodItem> database = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("food-database.json")) {
            if (is == null) {
                LOGGER.severe("Could not find food-database.json in resources");
                return database;
            }
            
            JsonNode root = mapper.readTree(is);
            JsonNode foods = root.get("foods");
            
            if (foods != null && foods.isArray()) {
                for (JsonNode foodNode : foods) {
                    FoodItem food = new FoodItem();
                    food.name = foodNode.get("name").asText();
                    food.calories = foodNode.get("calories").asDouble();
                    food.protein = foodNode.get("protein").asDouble();
                    food.fat = foodNode.get("fat").asDouble();
                    food.carbohydrate = foodNode.get("carbohydrate").asDouble();
                    
                    food.aliases = new ArrayList<>();
                    JsonNode aliasesNode = foodNode.get("aliases");
                    if (aliasesNode != null && aliasesNode.isArray()) {
                        for (JsonNode alias : aliasesNode) {
                            food.aliases.add(alias.asText());
                        }
                    }
                    
                    database.put(food.name, food);
                }
            }
            
            LOGGER.info("Loaded " + database.size() + " food items from database");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load food database", e);
        }
        
        return database;
    }
    
    private static class FoodItem {
        String name;
        List<String> aliases;
        double calories;
        double protein;
        double fat;
        double carbohydrate;
    }
}
