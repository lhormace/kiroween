package com.health.chat.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.health.chat.model.*;
import com.health.chat.repository.DataRepository;
import com.health.chat.repository.S3DataRepository;
import com.health.chat.service.*;
import com.health.chat.service.ErrorHandler;
import com.health.chat.service.InputValidator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda handler for chat dialogue processing.
 * Handles message processing, health data extraction, and advice generation.
 */
public class ChatHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AuthenticationService authService;
    private final MessageParser messageParser;
    private final HealthAdvisorAI healthAdvisor;
    private final MentalStateAnalyzer mentalStateAnalyzer;
    private final DataRepository dataRepository;
    private final ObjectMapper objectMapper;

    public ChatHandler() {
        // Initialize S3 client
        String bucketName = System.getenv("S3_BUCKET_NAME");
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = "health-chat-data";
        }

        S3Client s3Client = S3Client.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();

        this.dataRepository = new S3DataRepository(s3Client, bucketName);

        // Get JWT secret from environment variable
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set");
        }

        this.authService = new JwtAuthenticationService(dataRepository, jwtSecret);
        this.messageParser = new MessageParser();
        this.mentalStateAnalyzer = new KeywordBasedMentalStateAnalyzer();

        // Initialize MCP client
        String mcpEndpoint = System.getenv("MCP_ENDPOINT");
        String mcpTimeoutStr = System.getenv("MCP_TIMEOUT");
        int mcpTimeout = mcpTimeoutStr != null ? Integer.parseInt(mcpTimeoutStr) : 10;
        
        MCPClient mcpClient = null;
        if (mcpEndpoint != null && !mcpEndpoint.isEmpty()) {
            mcpClient = new HttpMCPClient(mcpEndpoint, mcpTimeout);
        }

        this.healthAdvisor = new MCPBasedHealthAdvisor(mcpClient);

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // Constructor for testing
    public ChatHandler(AuthenticationService authService, MessageParser messageParser,
                      HealthAdvisorAI healthAdvisor, MentalStateAnalyzer mentalStateAnalyzer,
                      DataRepository dataRepository) {
        this.authService = authService;
        this.messageParser = messageParser;
        this.healthAdvisor = healthAdvisor;
        this.mentalStateAnalyzer = mentalStateAnalyzer;
        this.dataRepository = dataRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ErrorHandler.logInfo("Processing chat request: " + input.getPath(), context);

        try {
            // Extract and validate token
            String token = extractToken(input);
            InputValidator.ValidationResult tokenValidation = InputValidator.validateToken(token);
            if (!tokenValidation.isValid() || !authService.validateToken(token)) {
                Map<String, Object> error = ErrorHandler.handleAuthenticationError(
                    "Invalid or missing authentication token", context, null);
                return createResponse(401, error);
            }

            String userId = authService.getUserIdFromToken(token);

            String path = input.getPath();
            String httpMethod = input.getHttpMethod();

            if ("/chat/message".equals(path) && "POST".equals(httpMethod)) {
                return handleMessage(input, userId, context);
            } else {
                return createResponse(404, ErrorHandler.createErrorResponse(
                    ErrorHandler.ErrorType.VALIDATION_ERROR, "Not found", "Endpoint not found"));
            }
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("chat request", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleMessage(APIGatewayProxyRequestEvent input, String userId, Context context) {
        try {
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);
            String message = body.get("message");

            // Validate message
            InputValidator.ValidationResult messageValidation = InputValidator.validateMessage(message);
            if (!messageValidation.isValid()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(messageValidation.getErrorMessage(), context);
                return createResponse(400, error);
            }

            // Parse message to extract health data
            HealthData healthData;
            try {
                healthData = messageParser.parseMessage(userId, message);
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = ErrorHandler.handleValidationError(e.getMessage(), context);
                return createResponse(400, error);
            }

            // Validate extracted health data
            if (healthData.getWeight() != null) {
                InputValidator.ValidationResult weightValidation = InputValidator.validateWeight(healthData.getWeight());
                if (!weightValidation.isValid()) {
                    Map<String, Object> error = ErrorHandler.handleValidationError(weightValidation.getErrorMessage(), context);
                    return createResponse(400, error);
                }
            }

            if (healthData.getBodyFatPercentage() != null) {
                InputValidator.ValidationResult bodyFatValidation = InputValidator.validateBodyFat(healthData.getBodyFatPercentage());
                if (!bodyFatValidation.isValid()) {
                    Map<String, Object> error = ErrorHandler.handleValidationError(bodyFatValidation.getErrorMessage(), context);
                    return createResponse(400, error);
                }
            }

            // Save health data with error handling
            try {
                dataRepository.saveHealthData(userId, healthData);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("save health data", context, e);
                return createResponse(500, error);
            }

            // Analyze mental state
            List<String> conversationHistory = getRecentConversationHistory(userId);
            MentalState mentalState = mentalStateAnalyzer.analyze(message, conversationHistory);

            // Save mental state with error handling
            try {
                dataRepository.saveMentalState(userId, LocalDate.now(), mentalState);
            } catch (Exception e) {
                ErrorHandler.logInfo("Failed to save mental state, continuing: " + e.getMessage(), context);
                // Non-critical, continue processing
            }

            // Get user profile
            UserProfile userProfile;
            try {
                userProfile = dataRepository.getUserProfile(userId);
            } catch (Exception e) {
                ErrorHandler.logInfo("Failed to retrieve user profile, using defaults: " + e.getMessage(), context);
                userProfile = null; // Use defaults
            }

            // Generate advice with MCP error handling
            AdviceResult advice;
            try {
                advice = healthAdvisor.generateAdvice(healthData, mentalState, userProfile);
            } catch (Exception e) {
                ErrorHandler.logInfo("MCP service unavailable, generating basic advice: " + e.getMessage(), context);
                // Generate basic advice without MCP
                advice = new AdviceResult();
                advice.setMainAdvice("データを記録しました。健康的な生活を続けましょう。");
                advice.setActionableRecommendations(List.of());
                advice.setReferences(List.of());
            }

            // Create response
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setResponseText("メッセージを受け取りました。");
            chatResponse.setExtractedData(healthData);
            chatResponse.setAdvice(advice.getMainAdvice());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("response", chatResponse);

            ErrorHandler.logSuccess("Message processed for user: " + userId, context);
            return createResponse(200, response);

        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("message processing", context, e);
            return createResponse(500, error);
        }
    }

    private String extractToken(APIGatewayProxyRequestEvent input) {
        // Try to get token from Authorization header
        Map<String, String> headers = input.getHeaders();
        if (headers != null) {
            String authHeader = headers.get("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }

    private List<String> getRecentConversationHistory(String userId) {
        try {
            // Get recent health data as conversation history
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(7);
            List<HealthData> recentData = dataRepository.getHealthDataByDateRange(userId, startDate, endDate);

            return recentData.stream()
                    .map(HealthData::getFreeComment)
                    .filter(comment -> comment != null && !comment.isEmpty())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);
            response.setBody(objectMapper.writeValueAsString(body));

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            response.setHeaders(headers);

            return response;
        } catch (Exception e) {
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setBody("{\"error\":\"Failed to create response\"}");
            return errorResponse;
        }
    }
}
