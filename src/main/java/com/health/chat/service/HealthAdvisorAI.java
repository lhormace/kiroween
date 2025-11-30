package com.health.chat.service;

import com.health.chat.model.AdviceResult;
import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.UserProfile;

public interface HealthAdvisorAI {
    AdviceResult generateAdvice(HealthData data, MentalState mentalState, UserProfile profile);
}
