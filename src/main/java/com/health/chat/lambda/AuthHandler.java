package com.health.chat.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.health.chat.model.AuthResult;
import com.health.chat.repository.DataRepository;
import com.health.chat.repository.S3DataRepository;
import com.health.chat.service.AuthenticationService;
import com.health.chat.service.ErrorHandler;
import com.health.chat.service.InputValidator;
import com.health.chat.service.JwtAuthenticationService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda handler for authentication operations.
 * Handles login, token validation, and logout requests.
 */
public class AuthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AuthenticationService authService;
    private final ObjectMapper objectMapper;

    public AuthHandler() {
        // Initialize S3 client
        String bucketName = System.getenv("S3_BUCKET_NAME");
        if (bucketName == null || bucketName.isEmpty()) {
            bucketName = "health-chat-data"; // Default bucket name
        }

        S3Client s3Client = S3Client.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                .build();

        DataRepository dataRepository = new S3DataRepository(s3Client, bucketName);

        // Get JWT secret from environment variable
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set");
        }

        this.authService = new JwtAuthenticationService(dataRepository, jwtSecret);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // Constructor for testing
    public AuthHandler(AuthenticationService authService) {
        this.authService = authService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        ErrorHandler.logInfo("Processing auth request: " + input.getPath(), context);

        try {
            String path = input.getPath();
            String httpMethod = input.getHttpMethod();

            if ("/auth/login".equals(path) && "POST".equals(httpMethod)) {
                return handleLogin(input, context);
            } else if ("/auth/validate".equals(path) && "POST".equals(httpMethod)) {
                return handleValidate(input, context);
            } else if ("/auth/logout".equals(path) && "POST".equals(httpMethod)) {
                return handleLogout(input, context);
            } else {
                return createResponse(404, ErrorHandler.createErrorResponse(
                    ErrorHandler.ErrorType.VALIDATION_ERROR, "Not found", "Endpoint not found"));
            }
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("auth request", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleLogin(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            // Validate username
            InputValidator.ValidationResult usernameValidation = InputValidator.validateUsername(username);
            if (!usernameValidation.isValid()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(usernameValidation.getErrorMessage(), context);
                return createResponse(400, error);
            }

            // Validate password
            InputValidator.ValidationResult passwordValidation = InputValidator.validatePassword(password);
            if (!passwordValidation.isValid()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(passwordValidation.getErrorMessage(), context);
                return createResponse(400, error);
            }

            AuthResult result = authService.authenticate(username, password);

            if (result.isSuccess()) {
                ErrorHandler.logSuccess("User login: " + username, context);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("token", result.getToken());
                response.put("userId", result.getUserId());
                return createResponse(200, response);
            } else {
                Map<String, Object> error = ErrorHandler.handleAuthenticationError(result.getErrorMessage(), context, null);
                return createResponse(401, error);
            }
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("login", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleValidate(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);
            String token = body.get("token");

            // Validate token format
            InputValidator.ValidationResult tokenValidation = InputValidator.validateToken(token);
            if (!tokenValidation.isValid()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(tokenValidation.getErrorMessage(), context);
                return createResponse(400, error);
            }

            boolean isValid = authService.validateToken(token);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                String userId = authService.getUserIdFromToken(token);
                response.put("userId", userId);
                ErrorHandler.logSuccess("Token validation", context);
            } else {
                ErrorHandler.logInfo("Token validation failed", context);
            }

            return createResponse(200, response);
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("token validation", context, e);
            return createResponse(500, error);
        }
    }

    private APIGatewayProxyResponseEvent handleLogout(APIGatewayProxyRequestEvent input, Context context) {
        try {
            Map<String, String> body = objectMapper.readValue(input.getBody(), Map.class);
            String token = body.get("token");

            // Validate token format
            InputValidator.ValidationResult tokenValidation = InputValidator.validateToken(token);
            if (!tokenValidation.isValid()) {
                Map<String, Object> error = ErrorHandler.handleValidationError(tokenValidation.getErrorMessage(), context);
                return createResponse(400, error);
            }

            authService.invalidateToken(token);
            ErrorHandler.logSuccess("User logout", context);
            return createResponse(200, Map.of("success", true, "message", "Logged out successfully"));
        } catch (Exception e) {
            Map<String, Object> error = ErrorHandler.handleInternalError("logout", context, e);
            return createResponse(500, error);
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
