package com.health.chat.model;

import java.time.LocalDateTime;

public class UserProfile {
    private String userId;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public UserProfile() {
    }

    public UserProfile(String userId, String username, String passwordHash, 
                      LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
