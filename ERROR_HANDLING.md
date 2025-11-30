# Error Handling and Logging Strategy

## Overview

This document describes the comprehensive error handling and logging implementation for the Health Chat Advisor system.

**Status**: ✅ Fully Implemented and Tested

All error handling and logging components have been implemented, tested, and verified to meet Requirements 8.5.

## Error Handling Components

### 1. ErrorHandler Utility (`ErrorHandler.java`)

Centralized error handling utility that provides:

- **Error Type Classification**: Categorizes errors into:
  - `AUTHENTICATION_ERROR`: Login, token validation failures
  - `VALIDATION_ERROR`: Input validation failures
  - `DATA_ACCESS_ERROR`: S3 and database operation failures
  - `EXTERNAL_SERVICE_ERROR`: MCP service failures
  - `INTERNAL_ERROR`: Unexpected system errors

- **Consistent Error Responses**: Standardized error response format with:
  - Error type
  - User-friendly error message
  - Technical details (logged but not exposed to users)

- **Logging Integration**: Logs to both Lambda Context logger (CloudWatch) and java.util.logging

### 2. InputValidator Utility (`InputValidator.java`)

Comprehensive input validation for:

- **Message Validation**: 
  - Maximum 140 characters
  - Non-empty content

- **Authentication Validation**:
  - Username: 3-50 characters, alphanumeric with underscore/hyphen
  - Password: 8-100 characters
  - Token: JWT format validation

- **Health Data Validation**:
  - Weight: 20-300 kg range
  - Body Fat: 3-60% range
  - Date: ISO format (YYYY-MM-DD)

- **Validation Results**: Returns structured validation results with detailed error messages

## Error Handling by Component

### Lambda Handlers

All Lambda handlers (AuthHandler, ChatHandler, AnalysisHandler) implement:

1. **Request-level error handling**: Catches all exceptions at the top level
2. **Authentication validation**: Validates tokens before processing
3. **Input validation**: Validates all user inputs before processing
4. **Operation-specific error handling**: Handles errors for each operation type
5. **Graceful degradation**: Continues with limited functionality when non-critical services fail

### Data Repository (S3DataRepository)

Implements robust data access error handling:

- **Retry Logic**: Up to 3 retries with exponential backoff (1s, 2s, 3s)
- **Retryable Errors**: Server errors (5xx) and throttling (429)
- **Error Logging**: Logs all retry attempts and final failures
- **Graceful Failure**: Throws descriptive exceptions after max retries

### MCP Client (HttpMCPClient)

Implements external service error handling:

- **Retry Logic**: Up to 2 retries for timeouts and server errors
- **Timeout Handling**: Configurable timeout (default 10 seconds)
- **Partial Success**: Returns available results if some domains fail
- **Detailed Logging**: Logs all MCP requests, responses, and failures

### Authentication Service (JwtAuthenticationService)

Implements secure authentication error handling:

- **Password Verification**: Uses BCrypt for secure password checking
- **Token Validation**: Validates JWT structure, signature, and expiration
- **Token Invalidation**: Maintains invalidated token list
- **Security Logging**: Logs authentication attempts without exposing sensitive data

## Logging Strategy

### Log Levels

- **INFO**: Successful operations, normal flow
- **WARNING**: Recoverable errors, degraded functionality
- **SEVERE**: Critical errors, operation failures

### Log Format

```
YYYY-MM-DD HH:MM:SS LEVEL [class.method] message
```

### CloudWatch Integration

All logs are automatically sent to CloudWatch Logs via Lambda's console output:

- Lambda execution logs
- Application logs (java.util.logging)
- Error stack traces (for internal errors)

### Log Retention

Configured in CDK stack (default: 30 days for cost optimization)

## Error Response Format

All error responses follow this structure:

```json
{
  "type": "ERROR_TYPE",
  "error": "Short error description",
  "message": "User-friendly error message"
}
```

Additional fields may be included for specific error types:
- `service`: For external service errors
- `s3ErrorCode`: For S3-specific errors
- `exceptionType`: For internal errors (debugging)

## HTTP Status Codes

- **200 OK**: Successful operation
- **400 Bad Request**: Validation errors, invalid input
- **401 Unauthorized**: Authentication failures
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: System errors, unexpected failures

## Retry Strategy

### S3 Operations
- Max retries: 3
- Backoff: Exponential (1s, 2s, 3s)
- Retryable: Server errors (5xx), throttling (429)

### MCP Operations
- Max retries: 2
- Backoff: Exponential (1s, 2s)
- Retryable: Timeouts, server errors (5xx)

### Non-retryable Errors
- Authentication failures
- Validation errors
- Client errors (4xx except 429)

## Graceful Degradation

The system continues operating with reduced functionality when:

1. **MCP Service Unavailable**: 
   - Generates basic advice without research references
   - Logs warning and continues

2. **Mental State Analysis Fails**:
   - Uses neutral mental state
   - Continues with advice generation

3. **User Profile Not Found**:
   - Uses default profile settings
   - Continues with advice generation

## Monitoring and Alerting

### CloudWatch Metrics

Monitor these key metrics:
- Lambda error rate
- Lambda duration
- S3 request failures
- MCP timeout rate

### CloudWatch Alarms

Recommended alarms:
- Lambda error rate > 5%
- Lambda duration > 25 seconds (approaching timeout)
- S3 5xx errors > 10 per minute

### Log Insights Queries

Useful queries for troubleshooting:

```
# Find all errors
fields @timestamp, @message
| filter @message like /ERROR|SEVERE/
| sort @timestamp desc

# Find authentication failures
fields @timestamp, @message
| filter @message like /Authentication failed/
| sort @timestamp desc

# Find MCP failures
fields @timestamp, @message
| filter @message like /MCP.*failed/
| sort @timestamp desc

# Find S3 retry attempts
fields @timestamp, @message
| filter @message like /Failed to.*attempt/
| sort @timestamp desc
```

## Best Practices

1. **Always validate input**: Use InputValidator for all user inputs
2. **Use ErrorHandler**: Use centralized error handling for consistency
3. **Log appropriately**: INFO for success, WARNING for recoverable errors, SEVERE for failures
4. **Don't expose internals**: Never expose stack traces or internal details to users
5. **Fail gracefully**: Continue with reduced functionality when possible
6. **Retry intelligently**: Only retry on transient errors
7. **Monitor actively**: Set up CloudWatch alarms for critical errors

## Testing Error Handling

### Unit Tests

Test error scenarios:
- Invalid inputs
- Authentication failures
- S3 failures (mock)
- MCP timeouts (mock)

### Integration Tests

Test end-to-end error flows:
- Invalid token handling
- Message length validation
- Data persistence failures
- External service unavailability

## Troubleshooting Guide

### High Error Rate

1. Check CloudWatch Logs for error patterns
2. Verify S3 bucket permissions
3. Check MCP service availability
4. Review recent code changes

### Slow Performance

1. Check Lambda duration metrics
2. Review S3 retry logs
3. Check MCP timeout rate
4. Optimize data access patterns

### Authentication Issues

1. Verify JWT secret configuration
2. Check token expiration settings
3. Review user profile data in S3
4. Verify BCrypt password hashes

## Configuration

### Environment Variables

Required for error handling:
- `JWT_SECRET`: For authentication
- `S3_BUCKET_NAME`: For data access
- `MCP_ENDPOINT`: For external service (optional)
- `MCP_TIMEOUT`: Timeout in seconds (optional, default: 10)

### Logging Configuration

Edit `src/main/resources/logging.properties` to adjust:
- Log levels per package
- Log format
- Console handler settings

## Future Enhancements

Potential improvements:
1. Distributed tracing with X-Ray
2. Custom CloudWatch metrics
3. Dead letter queue for failed operations
4. Circuit breaker for MCP service
5. Rate limiting for API endpoints
