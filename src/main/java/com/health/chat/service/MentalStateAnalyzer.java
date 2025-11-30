package com.health.chat.service;

import com.health.chat.model.MentalState;
import java.util.List;

/**
 * Interface for analyzing mental state from conversation content.
 * Analyzes dialogue to identify emotional indicators and motivation levels.
 */
public interface MentalStateAnalyzer {
    /**
     * Analyzes a message and conversation history to determine mental state.
     * 
     * @param message The current message to analyze
     * @param conversationHistory List of previous messages for context
     * @return MentalState containing tone, motivation level, and indicators
     */
    MentalState analyze(String message, List<String> conversationHistory);
}
