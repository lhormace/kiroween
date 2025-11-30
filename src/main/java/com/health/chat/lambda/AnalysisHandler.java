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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda handler for analysis operations.
 * Handles nutrition estimation, tanka generation, and historical data analysis.
 */
public class AnalysisHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AuthenticationService authService;
    private final NutritionEstimator nutritionEstimator;
    private final TankaGenerator tankaGenerator;
    private final MentalStateAnalyzer mentalStateAnalyzer;
    private final DataRepository dataRepository;
    private final ObjectMapper objectMapper;

    public AnalysisHandler() {
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
        this.nutritionEstimator = new BasicNutritionEstimator(dataRepository);
        this.tankaGenerator = new SimpleTankaGenerator();
        this.mentalStateAnalyzer = new KeywordBasedMentalStateAnalyzer();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // Constructor for testing
    public AnalysisHandler(AuthenticationService authService, NutritionEstimator nutritionEstimator,
                          TankaGenerator tankaGenerator, MentalStateAnalyzer mentalStateAnalyzer,
                          DataRepository dataRepository) {
        this.authService = authService;
        this.nutritionEstimator = nutritionEstimator;
        this.tankaGenerator = tankaGenerator;
        this.mentalStateAnalyzer = mentalStateAnalyzer;
        this.dataRepository = dataRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ErrorHandler.logInfo("Processing analysis request: " + input.getPath(), context);

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

            if ("/analysis/nutrition".equals(path) && "POST".equals(httpMethod)) {
                return handleNutritionAnalysis(input, userId, context);
            } else if ("/analysis/tanka".equals(path) && "POST".equals(httpMethod)) {
                return handleTankaGeneration(input, userId, context);
            } else if ("/analysis/tanka/history".equals(path) && "GET".equals(httpMethod)) {
                return handleTankaHistory(userId, context);
            } else if ("/analysis/daily".equals(path) && "GET".equals(httpMethod)) {
                return handleDailyAnalysis(input, userId, context);
            } else {
                return createResponse(404, ErrorHandler.createErrorResponse(
                    ErrorHandler.ErrorType.VALIDATION_ERROR, "Not found", "Endpoint not found"));
            }
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("analysis request", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleNutritionAnalysis(APIGatewayProxyRequestEvent input, String userId, Context context) {
        try {
            Map<String, Object> body = objectMapper.readValue(input.getBody(), Map.class);
            String dateStr = (String) body.get("date");
            
            // Validate date if provided
            if (dateStr != null) {
                InputValidator.ValidationResult dateValidation = InputValidator.validateDateString(dateStr);
                if (!dateValidation.isValid()) {
                    Map<String, Object> error = ErrorHandler.handleValidationError(dateValidation.getErrorMessage(), context);
                    return createResponse(400, error);
                }
            }
            
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            // Calculate daily nutrition with error handling
            DailyNutrition dailyNutrition;
            try {
                dailyNutrition = nutritionEstimator.calculateDailyTotal(userId, date);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("calculate nutrition", context, e);
                return createResponse(500, error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", date.toString());
            response.put("nutrition", dailyNutrition);

            ErrorHandler.logSuccess("Nutrition analysis for user: " + userId, context);
            return createResponse(200, response);

        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("nutrition analysis", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleTankaGeneration(APIGatewayProxyRequestEvent input, String userId, Context context) {
        try {
            Map<String, Object> body = objectMapper.readValue(input.getBody(), Map.class);
            String dateStr = (String) body.get("date");
            
            // Validate date if provided
            if (dateStr != null) {
                InputValidator.ValidationResult dateValidation = InputValidator.validateDateString(dateStr);
                if (!dateValidation.isValid()) {
                    Map<String, Object> error = ErrorHandler.handleValidationError(dateValidation.getErrorMessage(), context);
                    return createResponse(400, error);
                }
            }
            
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            // Get health data for the day with error handling
            List<HealthData> healthDataList;
            try {
                healthDataList = dataRepository.getHealthDataByDateRange(userId, date, date);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("retrieve health data", context, e);
                return createResponse(500, error);
            }
            
            if (healthDataList.isEmpty()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(
                    "No health data found for the specified date", context);
                return createResponse(404, error);
            }

            HealthData healthData = healthDataList.get(0);

            // Get mental state for the day
            MentalState mentalState;
            try {
                mentalState = dataRepository.getMentalState(userId, date);
            } catch (Exception e) {
                ErrorHandler.logInfo("No mental state found, using neutral: " + e.getMessage(), context);
                // If no mental state found, create a neutral one
                mentalState = new MentalState(EmotionalTone.NEUTRAL, 0.5, List.of());
            }

            // Generate tanka
            TankaPoem tanka = tankaGenerator.generate(healthData, mentalState);

            // Save tanka with error handling
            try {
                dataRepository.saveTanka(userId, tanka);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("save tanka", context, e);
                return createResponse(500, error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tanka", tanka);

            ErrorHandler.logSuccess("Tanka generation for user: " + userId, context);
            return createResponse(200, response);

        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("tanka generation", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleTankaHistory(String userId, Context context) {
        try {
            List<TankaPoem> tankaHistory;
            try {
                tankaHistory = dataRepository.getTankaHistory(userId);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("retrieve tanka history", context, e);
                return createResponse(500, error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tankas", tankaHistory);

            ErrorHandler.logSuccess("Tanka history retrieval for user: " + userId, context);
            return createResponse(200, response);

        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("tanka history retrieval", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleDailyAnalysis(APIGatewayProxyRequestEvent input, String userId, Context context) {
        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            String dateStr = queryParams != null ? queryParams.get("date") : null;
            
            // Validate date if provided
            if (dateStr != null) {
                InputValidator.ValidationResult dateValidation = InputValidator.validateDateString(dateStr);
                if (!dateValidation.isValid()) {
                    Map<String, Object> error = ErrorHandler.handleValidationError(dateValidation.getErrorMessage(), context);
                    return createResponse(400, error);
                }
            }
            
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

            // Get health data with error handling
            List<HealthData> healthDataList;
            try {
                healthDataList = dataRepository.getHealthDataByDateRange(userId, date, date);
            } catch (Exception e) {
                Map<String, Object> error = ErrorHandler.handleDataAccessError("retrieve health data", context, e);
                return createResponse(500, error);
            }

            // Get nutrition data with error handling
            DailyNutrition nutrition;
            try {
                nutrition = nutritionEstimator.calculateDailyTotal(userId, date);
            } catch (Exception e) {
                ErrorHandler.logInfo("Failed to calculate nutrition, using defaults: " + e.getMessage(), context);
                nutrition = null; // Will be null in response
            }

            // Get mental state (optional)
            MentalState mentalState = null;
            try {
                mentalState = dataRepository.getMentalState(userId, date);
            } catch (Exception e) {
                ErrorHandler.logInfo("No mental state found for date: " + date, context);
                // Mental state might not exist, this is okay
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", date.toString());
            response.put("healthData", healthDataList);
            response.put("nutrition", nutrition);
            response.put("mentalState", mentalState);

            ErrorHandler.logSuccess("Daily analysis for user: " + userId, context);
            return createResponse(200, response);

        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("daily analysis", context, e);
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
