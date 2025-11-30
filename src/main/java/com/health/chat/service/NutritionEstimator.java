package com.health.chat.service;

import com.health.chat.model.DailyNutrition;
import com.health.chat.model.NutritionInfo;

import java.time.LocalDate;
import java.util.List;

public interface NutritionEstimator {
    NutritionInfo estimateNutrition(List<String> foodItems);
    DailyNutrition calculateDailyTotal(String userId, LocalDate date);
}
