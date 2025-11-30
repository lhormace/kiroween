package com.health.chat.repository;

import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.NutritionInfo;
import com.health.chat.model.TankaPoem;

import java.time.LocalDate;
import java.util.List;

public interface DataRepository {
    void saveHealthData(String userId, HealthData data);
    List<HealthData> getHealthDataByDateRange(String userId, LocalDate start, LocalDate end);
    void saveNutritionInfo(String userId, LocalDate date, NutritionInfo info);
    void saveMentalState(String userId, LocalDate date, MentalState state);
    void saveTanka(String userId, TankaPoem tanka);
    List<TankaPoem> getTankaHistory(String userId);
}
