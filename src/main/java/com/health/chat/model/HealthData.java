package com.health.chat.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HealthData {
    private String userId;
    private LocalDate date;
    private LocalDateTime timestamp;
    private Double weight;
    private Double bodyFatPercentage;
    private List<String> foodItems;
    private List<String> exercises;
    private String freeComment;

    public HealthData() {
    }

    public HealthData(String userId, LocalDate date, LocalDateTime timestamp, 
                     Double weight, Double bodyFatPercentage, 
                     List<String> foodItems, List<String> exercises, String freeComment) {
        this.userId = userId;
        this.date = date;
        this.timestamp = timestamp;
        this.weight = weight;
        this.bodyFatPercentage = bodyFatPercentage;
        this.foodItems = foodItems;
        this.exercises = exercises;
        this.freeComment = freeComment;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getBodyFatPercentage() {
        return bodyFatPercentage;
    }

    public void setBodyFatPercentage(Double bodyFatPercentage) {
        this.bodyFatPercentage = bodyFatPercentage;
    }

    public List<String> getFoodItems() {
        return foodItems;
    }

    public void setFoodItems(List<String> foodItems) {
        this.foodItems = foodItems;
    }

    public List<String> getExercises() {
        return exercises;
    }

    public void setExercises(List<String> exercises) {
        this.exercises = exercises;
    }

    public String getFreeComment() {
        return freeComment;
    }

    public void setFreeComment(String freeComment) {
        this.freeComment = freeComment;
    }
}
