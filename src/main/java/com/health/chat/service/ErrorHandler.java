package com.health.chat.service;

import com.amazonaws.services.lambda.runtime.Context;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized error handling utility for the health chat application.
 * Provides consistent error handling, logging, and response formatting.
 */
public class ErrorHandler {
    
    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());
    
    /**
     * Error types for categorization
     */
    public enum ErrorType {
        AUTHENTICATION_ERROR,
        VALIDATION_ERROR,
        DATA_ACCESS_ERROR,
        EXTERNAL_SERVICE_ERROR,
        INTERNAL_ERROR
    }
    
    /**
     * Handles authentication errors
     */
    public static Map<String, Object> handleAuthenticationError(String message, Context context, Exception e) {
        if (context != null) {
            context.getLogger().log("Authentication error: " + message);
        }
        LOGGER.log(Level.WARNING, "Authentication error: " + message, e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Authentication failed");
        error.put("message", message);
        error.put("type", ErrorType.AUTHENTICATION_ERROR.name());
        return error;
    }
    
    /**
     * Handles input validation errors
     */
    public static Map<String, Object> handleValidationError(String message, Context context) {
        if (context != null) {
            context.getLogger().log("Validation error: " + message);
        }
        LOGGER.log(Level.INFO, "Validation error: " + message);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Validation failed");
        error.put("message", message);
        error.put("type", ErrorType.VALIDATION_ERROR.name());
        return error;
    }
    
    /**
     * Handles data access errors with retry information
     */
    public static Map<String, Object> handleDataAccessError(String operation, Context context, Exception e) {
        String message = String.format("Data access failed for operation: %s", operation);
        
        if (context != null) {
            context.getLogger().log(message + " - " + e.getMessage());
        }
        LOGGER.log(Level.SEVERE, message, e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Data access failed");
        error.put("message", "Failed to " + operation + ". Please try again.");
        error.put("type", ErrorType.DATA_ACCESS_ERROR.name());
        
        // Add specific S3 error information if available
        if (e instanceof S3Exception) {
            S3Exception s3e = (S3Exception) e;
            if (s3e.awsErrorDetails() != null) {
                error.put("s3ErrorCode", s3e.awsErrorDetails().errorCode());
            }
        }
        
        return error;
    }
    
    /**
     * Handles external service errors (e.g., MCP)
     */
    public static Map<String, Object> handleExternalServiceError(String service, Context context, Exception e) {
        String message = String.format("External service error: %s", service);
        
        if (context != null) {
            context.getLogger().log(message + " - " + e.getMessage());
        }
        LOGGER.log(Level.WARNING, message, e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "External service unavailable");
        error.put("message", String.format("%s is temporarily unavailable. Continuing with limited functionality.", service));
        error.put("type", ErrorType.EXTERNAL_SERVICE_ERROR.name());
        error.put("service", service);
        
        return error;
    }
    
    /**
     * Handles unexpected internal errors
     */
    public static Map<String, Object> handleInternalError(String operation, Context context, Exception e) {
        String message = String.format("Internal error during: %s", operation);
        
        if (context != null) {
            context.getLogger().log(message + " - " + e.getMessage());
            // Log full stack trace for internal errors
            if (e != null) {
                for (StackTraceElement element : e.getStackTrace()) {
                    context.getLogger().log("  at " + element.toString());
                }
            }
        }
        LOGGER.log(Level.SEVERE, message, e);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", "An unexpected error occurred. Please try again later.");
        error.put("type", ErrorType.INTERNAL_ERROR.name());
        
        // Include exception class for debugging (but not the full message for security)
        if (e != null) {
            error.put("exceptionType", e.getClass().getSimpleName());
        }
        
        return error;
    }
    
    /**
     * Logs successful operations
     */
    public static void logSuccess(String operation, Context context) {
        if (context != null) {
            context.getLogger().log("Success: " + operation);
        }
        LOGGER.log(Level.INFO, "Success: " + operation);
    }
    
    /**
     * Logs informational messages
     */
    public static void logInfo(String message, Context context) {
        if (context != null) {
            context.getLogger().log("Info: " + message);
        }
        LOGGER.log(Level.INFO, message);
    }
    
    /**
     * Determines if an exception is retryable
     */
    public static boolean isRetryable(Exception e) {
        if (e instanceof S3Exception) {
            S3Exception s3e = (S3Exception) e;
            int statusCode = s3e.statusCode();
            // Retry on server errors (5xx) and throttling (429)
            return statusCode >= 500 || statusCode == 429;
        }
        
        if (e instanceof MCPException) {
            // MCP timeouts are retryable
            return e.getMessage() != null && e.getMessage().contains("timed out");
        }
        
        return false;
    }
    
    /**
     * Creates a standardized error response map
     */
    public static Map<String, Object> createErrorResponse(ErrorType type, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", type.name());
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}
