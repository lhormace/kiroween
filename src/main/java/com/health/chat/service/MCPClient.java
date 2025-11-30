package com.health.chat.service;

import com.health.chat.model.ResearchReference;
import java.util.List;

/**
 * MCP Client interface for fetching research information
 * from nutrition, sleep, and exercise physiology domains.
 */
public interface MCPClient {
    
    /**
     * Fetches nutrition research information based on the query.
     * 
     * @param query The nutrition-related query
     * @return List of research references related to nutrition
     * @throws MCPException if the MCP service is unavailable or times out
     */
    List<ResearchReference> fetchNutritionResearch(String query) throws MCPException;
    
    /**
     * Fetches sleep research information based on the query.
     * 
     * @param query The sleep-related query
     * @return List of research references related to sleep
     * @throws MCPException if the MCP service is unavailable or times out
     */
    List<ResearchReference> fetchSleepResearch(String query) throws MCPException;
    
    /**
     * Fetches exercise physiology research information based on the query.
     * 
     * @param query The exercise-related query
     * @return List of research references related to exercise physiology
     * @throws MCPException if the MCP service is unavailable or times out
     */
    List<ResearchReference> fetchExerciseResearch(String query) throws MCPException;
    
    /**
     * Fetches all relevant research (nutrition, sleep, exercise) for a comprehensive query.
     * 
     * @param query The health-related query
     * @return List of research references from all domains
     * @throws MCPException if the MCP service is unavailable or times out
     */
    List<ResearchReference> fetchAllResearch(String query) throws MCPException;
}
