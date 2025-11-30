package com.health.chat.service;

import com.health.chat.model.MentalState;

import java.util.List;

public interface MentalStateAnalyzer {
    MentalState analyze(String message, List<String> conversationHistory);
}
