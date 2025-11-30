package com.health.chat.service;

import com.health.chat.model.HealthData;
import com.health.chat.model.TimeRange;

import java.util.List;

public interface GraphGenerator {
    byte[] generateGraph(List<HealthData> data, TimeRange range);
}
