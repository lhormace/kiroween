package com.health.chat.service;

import com.health.chat.model.AuthResult;
import com.health.chat.model.UserProfile;
import com.health.chat.repository.DataRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JwtAuthenticationService implements AuthenticationService {
    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationService.class.getName());
    private static final long TOKEN_VALIDITY_HOURS = 24;
    
    private final DataRepository dataRepository;
    private final SecretKey secretKey;
    private final Set<String> invalidatedTokens;

    public JwtAuthenticationService(DataRepository dataRepository, String jwtSecret) {
        this.dataRepository = dataRepository;
        // Ensure the secret is at least 256 bits (32 bytes) for HS256
        String paddedSecret = jwtSecret.length() >= 32 ? jwtSecret : 
            String.format("%-32s", jwtSecret).replace(' ', '0');
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        this.invalidatedTokens = new HashSet<>();
    }

    @Override
    public AuthResult authenticate(String username, String password) {
        try {
            // Retrieve user profile by username
            UserProfile profile = dataRepository.getUserProfileByUsername(username);
            
            if (profile == null) {
                LOGGER.log(Level.INFO, "Authentication failed: user not found - " + username);
                return new AuthResult(false, null, null, "Invalid username or password");
            }
            
            // Verify password using BCrypt
            if (!BCrypt.checkpw(password, profile.getPasswordHash())) {
                LOGGER.log(Level.INFO, "Authentication failed: invalid password for user - " + username);
                return new AuthResult(false, null, null, "Invalid username or password");
            }
            
            // Update last login time
            profile.setLastLoginAt(LocalDateTime.now());
            dataRepository.saveUserProfile(profile);
            
            // Generate JWT token
            String token = generateToken(profile.getUserId(), username);
            
            LOGGER.log(Level.INFO, "Authentication successful for user: " + username);
            return new AuthResult(true, token, profile.getUserId(), null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error for user: " + username, e);
            return new AuthResult(false, null, null, "Authentication service error");
        }
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Check if token has been invalidated
        if (invalidatedTokens.contains(token)) {
            LOGGER.log(Level.INFO, "Token validation failed: token has been invalidated");
            return false;
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // Check expiration
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                LOGGER.log(Level.INFO, "Token validation failed: token has expired");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Token validation failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void invalidateToken(String token) {
        if (token != null && !token.isEmpty()) {
            invalidatedTokens.add(token);
            LOGGER.log(Level.INFO, "Token invalidated");
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to extract userId from token: " + e.getMessage());
            return null;
        }
    }

    private String generateToken(String userId, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);
        
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Utility method to hash a password using BCrypt
     * This can be used when creating new users
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}
