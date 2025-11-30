package com.health.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.chat.model.ResearchReference;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP-based implementation of MCP Client.
 * Connects to MCP servers to fetch research information.
 */
public class HttpMCPClient implements MCPClient {
    
    private static final Logger LOGGER = Logger.getLogger(HttpMCPClient.class.getName());
    private static final int MAX_RETRIES = 2;
    
    private final String mcpEndpoint;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    
    /**
     * Creates an MCP client with default settings.
     * Default endpoint: http://localhost:3000/mcp
     * Default timeout: 10 seconds
     */
    public HttpMCPClient() {
        this("http://localhost:3000/mcp", 10);
    }
    
    /**
     * Creates an MCP client with custom endpoint and timeout.
     * 
     * @param mcpEndpoint The MCP server endpoint URL
     * @param timeoutSeconds Timeout in seconds for MCP requests
     */
    public HttpMCPClient(String mcpEndpoint, int timeoutSeconds) {
        this.mcpEndpoint = mcpEndpoint;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
        
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .setResponseTimeout(Timeout.of(timeoutSeconds, TimeUnit.SECONDS))
            .build();
        
        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .build();
    }
    
    @Override
    public List<ResearchReference> fetchNutritionResearch(String query) throws MCPException {
        return fetchResearch("nutrition", query);
    }
    
    @Override
    public List<ResearchReference> fetchSleepResearch(String query) throws MCPException {
        return fetchResearch("sleep", query);
    }
    
    @Override
    public List<ResearchReference> fetchExerciseResearch(String query) throws MCPException {
        return fetchResearch("exercise", query);
    }
    
    @Override
    public List<ResearchReference> fetchAllResearch(String query) throws MCPException {
        List<ResearchReference> allReferences = new ArrayList<>();
        List<String> failedDomains = new ArrayList<>();
        
        try {
            allReferences.addAll(fetchNutritionResearch(query));
        } catch (MCPException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch nutrition research: " + e.getMessage());
            failedDomains.add("nutrition");
        }
        
        try {
            allReferences.addAll(fetchSleepResearch(query));
        } catch (MCPException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch sleep research: " + e.getMessage());
            failedDomains.add("sleep");
        }
        
        try {
            allReferences.addAll(fetchExerciseResearch(query));
        } catch (MCPException e) {
            LOGGER.log(Level.WARNING, "Failed to fetch exercise research: " + e.getMessage());
            failedDomains.add("exercise");
        }
        
        if (allReferences.isEmpty()) {
            String message = "Failed to fetch research from all domains: " + String.join(", ", failedDomains);
            LOGGER.log(Level.SEVERE, message);
            throw new MCPException(message);
        }
        
        if (!failedDomains.isEmpty()) {
            LOGGER.log(Level.INFO, "Partial success: failed domains: " + String.join(", ", failedDomains));
        }
        
        return allReferences;
    }
    
    /**
     * Internal method to fetch research for a specific topic with retry logic.
     */
    private List<ResearchReference> fetchResearch(String topic, String query) throws MCPException {
        int attempts = 0;
        MCPException lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                return attemptFetchResearch(topic, query);
            } catch (MCPException e) {
                lastException = e;
                attempts++;
                
                // Only retry on timeout or server errors
                if (e.getMessage().contains("timed out") || e.getMessage().contains("status 5")) {
                    if (attempts < MAX_RETRIES) {
                        LOGGER.log(Level.WARNING, 
                            String.format("MCP request failed (attempt %d/%d) for topic %s: %s", 
                                attempts, MAX_RETRIES, topic, e.getMessage()));
                        try {
                            Thread.sleep(1000 * attempts); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new MCPException("Interrupted during retry", ie);
                        }
                    }
                } else {
                    // Don't retry on client errors
                    throw e;
                }
            }
        }
        
        LOGGER.log(Level.SEVERE, "MCP request failed after " + MAX_RETRIES + " attempts for topic: " + topic);
        throw lastException;
    }
    
    /**
     * Single attempt to fetch research for a specific topic.
     */
    private List<ResearchReference> attemptFetchResearch(String topic, String query) throws MCPException {
        HttpPost request = new HttpPost(mcpEndpoint);
        
        try {
            // Create JSON request body
            String jsonRequest = objectMapper.writeValueAsString(
                new MCPRequest(topic, query)
            );
            
            request.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));
            request.setHeader("Content-Type", "application/json");
            
            LOGGER.log(Level.INFO, "Sending MCP request for topic: " + topic);
            
            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                
                if (statusCode == 200) {
                    String responseBody = new String(
                        response.getEntity().getContent().readAllBytes()
                    );
                    List<ResearchReference> results = parseResponse(topic, responseBody);
                    LOGGER.log(Level.INFO, "Successfully fetched " + results.size() + " references for topic: " + topic);
                    return results;
                } else if (statusCode == 408 || statusCode == 504) {
                    throw new MCPException("MCP request timed out for topic: " + topic);
                } else if (statusCode >= 500) {
                    throw new MCPException(
                        "MCP server error (status " + statusCode + ") for topic: " + topic
                    );
                } else {
                    throw new MCPException(
                        "MCP request failed with status " + statusCode + " for topic: " + topic
                    );
                }
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to MCP service for topic: " + topic, e);
            throw new MCPException("Failed to connect to MCP service for topic: " + topic, e);
        }
    }
    
    /**
     * Parses the MCP response and extracts research references.
     */
    private List<ResearchReference> parseResponse(String topic, String responseBody) throws MCPException {
        List<ResearchReference> references = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultsNode = root.get("results");
            
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode resultNode : resultsNode) {
                    String summary = resultNode.get("summary").asText();
                    String source = resultNode.has("source") 
                        ? resultNode.get("source").asText() 
                        : "MCP Research Database";
                    
                    references.add(new ResearchReference(topic, summary, source));
                }
            }
            
            return references;
            
        } catch (Exception e) {
            throw new MCPException("Failed to parse MCP response for topic: " + topic, e);
        }
    }
    
    /**
     * Closes the HTTP client and releases resources.
     */
    public void close() {
        try {
            httpClient.close();
            LOGGER.log(Level.INFO, "HTTP client closed successfully");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing HTTP client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Internal class for MCP request structure.
     */
    private static class MCPRequest {
        public String topic;
        public String query;
        
        public MCPRequest(String topic, String query) {
            this.topic = topic;
            this.query = query;
        }
    }
}
