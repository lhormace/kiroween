package com.health.chat.service;

import com.health.chat.model.HealthData;
import com.health.chat.model.MentalState;
import com.health.chat.model.TankaPoem;

public interface TankaGenerator {
    TankaPoem generate(HealthData data, MentalState mentalState);
}
