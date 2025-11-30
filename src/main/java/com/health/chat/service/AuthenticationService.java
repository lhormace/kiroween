package com.health.chat.service;

import com.health.chat.model.AuthResult;

public interface AuthenticationService {
    AuthResult authenticate(String username, String password);
    boolean validateToken(String token);
    void invalidateToken(String token);
    String getUserIdFromToken(String token);
}
