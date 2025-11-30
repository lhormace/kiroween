package com.health.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.chat.model.HealthData;
import com.health.chat.model.TimeRange;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChartJsGraphGenerator implements GraphGenerator {
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ChartJsGraphGenerator() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public byte[] generateGraph(List<HealthData> data, TimeRange range) {
        if (data == null || data.isEmpty()) {
            return generateEmptyGraph();
        }

        // Sort data by date
        List<HealthData> sortedData = data.stream()
                .sorted(Comparator.comparing(HealthData::getDate))
                .collect(Collectors.toList());

        // Prepare data for Chart.js
        Map<String, Object> chartData = new HashMap<>();
        
        List<String> labels = new ArrayList<>();
        List<Double> weightData = new ArrayList<>();
        List<Double> bodyFatData = new ArrayList<>();

        for (HealthData healthData : sortedData) {
            labels.add(healthData.getDate().format(DATE_FORMATTER));
            weightData.add(healthData.getWeight() != null ? healthData.getWeight() : null);
            bodyFatData.add(healthData.getBodyFatPercentage() != null ? healthData.getBodyFatPercentage() : null);
        }

        chartData.put("labels", labels);
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        
        // Weight dataset
        Map<String, Object> weightDataset = new HashMap<>();
        weightDataset.put("label", "体重 (kg)");
        weightDataset.put("data", weightData);
        weightDataset.put("borderColor", "rgb(102, 126, 234)");
        weightDataset.put("backgroundColor", "rgba(102, 126, 234, 0.1)");
        weightDataset.put("yAxisID", "y");
        weightDataset.put("tension", 0.4);
        datasets.add(weightDataset);
        
        // Body fat dataset
        Map<String, Object> bodyFatDataset = new HashMap<>();
        bodyFatDataset.put("label", "体脂肪率 (%)");
        bodyFatDataset.put("data", bodyFatData);
        bodyFatDataset.put("borderColor", "rgb(118, 75, 162)");
        bodyFatDataset.put("backgroundColor", "rgba(118, 75, 162, 0.1)");
        bodyFatDataset.put("yAxisID", "y1");
        bodyFatDataset.put("tension", 0.4);
        datasets.add(bodyFatDataset);
        
        chartData.put("datasets", datasets);

        try {
            return objectMapper.writeValueAsBytes(chartData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate graph data", e);
        }
    }

    private byte[] generateEmptyGraph() {
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", Collections.emptyList());
        chartData.put("datasets", Collections.emptyList());
        
        try {
            return objectMapper.writeValueAsBytes(chartData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate empty graph data", e);
        }
    }
}
