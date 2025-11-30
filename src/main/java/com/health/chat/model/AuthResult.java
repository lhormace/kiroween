package com.health.chat.model;

public class AuthResult {
    private boolean success;
    private String token;
    private String userId;
    private String errorMessage;

    public AuthResult() {
    }

    public AuthResult(boolean success, String token, String userId, String errorMessage) {
        this.success = success;
        this.token = token;
        this.userId = userId;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
