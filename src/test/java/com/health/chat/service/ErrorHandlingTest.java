package com.health.chat.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling and logging functionality.
 * Validates Requirements 8.5: Error handling and logging
 */
public class ErrorHandlingTest {

    private static class TestContext implements Context {
        private final StringBuilder logs = new StringBuilder();
        
        @Override
        public String getAwsRequestId() { return "test-request-id"; }
        
        @Override
        public String getLogGroupName() { return "test-log-group"; }
        
        @Override
        public String getLogStreamName() { return "test-log-stream"; }
        
        @Override
        public String getFunctionName() { return "test-function"; }
        
        @Override
        public String getFunctionVersion() { return "1"; }
        
        @Override
        public String getInvokedFunctionArn() { return "arn:aws:lambda:us-east-1:123456789012:function:test"; }
        
        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() { return null; }
        
        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() { return null; }
        
        @Override
        public int getRemainingTimeInMillis() { return 30000; }
        
        @Override
        public int getMemoryLimitInMB() { return 512; }
        
        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                    logs.append(message).append("\n");
                }
                
                @Override
                public void log(byte[] message) {
                    logs.append(new String(message)).append("\n");
                }
            };
        }
        
        public String getLogs() {
            return logs.toString();
        }
    }

    @Test
    public void testAuthenticationErrorHandling() {
        TestContext context = new TestContext();
        Exception testException = new RuntimeException("Invalid credentials");
        
        Map<String, Object> error = ErrorHandler.handleAuthenticationError(
            "User authentication failed", context, testException);
        
        // Verify error structure
        assertEquals("Authentication failed", error.get("error"));
        assertEquals("User authentication failed", error.get("message"));
        assertEquals(ErrorHandler.ErrorType.AUTHENTICATION_ERROR.name(), error.get("type"));
        
        // Verify logging occurred
        assertTrue(context.getLogs().contains("Authentication error"));
    }

    @Test
    public void testValidationErrorHandling() {
        TestContext context = new TestContext();
        
        Map<String, Object> error = ErrorHandler.handleValidationError(
            "Message exceeds maximum length", context);
        
        // Verify error structure
        assertEquals("Validation failed", error.get("error"));
        assertEquals("Message exceeds maximum length", error.get("message"));
        assertEquals(ErrorHandler.ErrorType.VALIDATION_ERROR.name(), error.get("type"));
        
        // Verify logging occurred
        assertTrue(context.getLogs().contains("Validation error"));
    }

    @Test
    public void testDataAccessErrorHandling() {
        TestContext context = new TestContext();
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied")
            .statusCode(403)
            .build();
        
        Map<String, Object> error = ErrorHandler.handleDataAccessError(
            "save health data", context, s3Exception);
        
        // Verify error structure
        assertEquals("Data access failed", error.get("error"));
        assertTrue(error.get("message").toString().contains("save health data"));
        assertEquals(ErrorHandler.ErrorType.DATA_ACCESS_ERROR.name(), error.get("type"));
        
        // Verify logging occurred
        assertTrue(context.getLogs().contains("Data access failed"));
    }

    @Test
    public void testExternalServiceErrorHandling() {
        TestContext context = new TestContext();
        MCPException mcpException = new MCPException("MCP service timeout");
        
        Map<String, Object> error = ErrorHandler.handleExternalServiceError(
            "MCP", context, mcpException);
        
        // Verify error structure
        assertEquals("External service unavailable", error.get("error"));
        assertTrue(error.get("message").toString().contains("temporarily unavailable"));
        assertEquals(ErrorHandler.ErrorType.EXTERNAL_SERVICE_ERROR.name(), error.get("type"));
        assertEquals("MCP", error.get("service"));
        
        // Verify logging occurred
        assertTrue(context.getLogs().contains("External service error"));
    }

    @Test
    public void testInternalErrorHandling() {
        TestContext context = new TestContext();
        NullPointerException npe = new NullPointerException("Unexpected null value");
        
        Map<String, Object> error = ErrorHandler.handleInternalError(
            "process message", context, npe);
        
        // Verify error structure
        assertEquals("Internal server error", error.get("error"));
        assertEquals("An unexpected error occurred. Please try again later.", error.get("message"));
        assertEquals(ErrorHandler.ErrorType.INTERNAL_ERROR.name(), error.get("type"));
        assertEquals("NullPointerException", error.get("exceptionType"));
        
        // Verify logging occurred
        assertTrue(context.getLogs().contains("Internal error"));
    }

    @Test
    public void testRetryableErrorDetection() {
        // S3 server errors should be retryable
        S3Exception serverError = (S3Exception) S3Exception.builder()
            .message("Internal server error")
            .statusCode(500)
            .build();
        assertTrue(ErrorHandler.isRetryable(serverError));
        
        // S3 throttling should be retryable
        S3Exception throttling = (S3Exception) S3Exception.builder()
            .message("Too many requests")
            .statusCode(429)
            .build();
        assertTrue(ErrorHandler.isRetryable(throttling));
        
        // S3 client errors should not be retryable
        S3Exception clientError = (S3Exception) S3Exception.builder()
            .message("Access denied")
            .statusCode(403)
            .build();
        assertFalse(ErrorHandler.isRetryable(clientError));
        
        // MCP timeouts should be retryable
        MCPException timeout = new MCPException("Request timed out");
        assertTrue(ErrorHandler.isRetryable(timeout));
        
        // MCP other errors should not be retryable
        MCPException otherError = new MCPException("Invalid request");
        assertFalse(ErrorHandler.isRetryable(otherError));
    }

    @Test
    public void testSuccessLogging() {
        TestContext context = new TestContext();
        
        ErrorHandler.logSuccess("User login", context);
        
        assertTrue(context.getLogs().contains("Success: User login"));
    }

    @Test
    public void testInfoLogging() {
        TestContext context = new TestContext();
        
        ErrorHandler.logInfo("Processing request", context);
        
        assertTrue(context.getLogs().contains("Info: Processing request"));
    }

    @Test
    public void testCreateErrorResponse() {
        Map<String, Object> response = ErrorHandler.createErrorResponse(
            ErrorHandler.ErrorType.VALIDATION_ERROR,
            "Invalid input",
            "The provided input is invalid"
        );
        
        assertEquals(ErrorHandler.ErrorType.VALIDATION_ERROR.name(), response.get("type"));
        assertEquals("Invalid input", response.get("error"));
        assertEquals("The provided input is invalid", response.get("message"));
    }
}
