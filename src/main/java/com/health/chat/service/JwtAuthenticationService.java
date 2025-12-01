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
        // Use thread-safe Set for concurrent access
        this.invalidatedTokens = java.util.concurrent.ConcurrentHashMap.newKeySet();
    }

    @Override
    public AuthResult authenticate(String username, String password) {
        try {
            // Retrieve user profile by username
            UserProfile profile = dataRepository.getUserProfileByUsername(username);
            
            if (profile == null) {
                LOGGER.log(Level.INFO, "Authentication failed: user not found");
                return new AuthResult(false, null, null, "Invalid username or password");
            }
            
            // Verify password using BCrypt
            if (!BCrypt.checkpw(password, profile.getPasswordHash())) {
                LOGGER.log(Level.INFO, "Authentication failed: invalid password");
                return new AuthResult(false, null, null, "Invalid username or password");
            }
            
            // Update last login time
            profile.setLastLoginAt(LocalDateTime.now());
            dataRepository.saveUserProfile(profile);
            
            // Generate JWT token
            String token = generateToken(profile.getUserId(), username);
            
            LOGGER.log(Level.INFO, "Authentication successful");
            return new AuthResult(true, token, profile.getUserId(), null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error", e);
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
     * Register a new user with username, password, and email.
     * 
     * @param username The desired username (must be unique)
     * @param password The plain text password (will be hashed)
     * @param email The user's email address
     * @return AuthResult with success status and token if successful
     */
    public AuthResult registerUser(String username, String password, String email) {
        try {
            // Validate input
            if (username == null || username.trim().isEmpty()) {
                return new AuthResult(false, null, null, "Username cannot be empty");
            }
            
            // Username validation: 3-20 characters, alphanumeric and underscore only
            String trimmedUsername = username.trim();
            if (trimmedUsername.length() < 3 || trimmedUsername.length() > 20) {
                return new AuthResult(false, null, null, "Username must be between 3 and 20 characters");
            }
            if (!trimmedUsername.matches("^[a-zA-Z0-9_]+$")) {
                return new AuthResult(false, null, null, "Username can only contain letters, numbers, and underscores");
            }
            
            // Password validation
            if (password == null || password.length() < 8) {
                return new AuthResult(false, null, null, "Password must be at least 8 characters");
            }
            if (password.length() > 128) {
                return new AuthResult(false, null, null, "Password is too long");
            }
            
            // Email validation with regex
            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return new AuthResult(false, null, null, "Invalid email address");
            }
            
            // Check if username already exists
            UserProfile existingUser = dataRepository.getUserProfileByUsername(trimmedUsername);
            if (existingUser != null) {
                LOGGER.log(Level.INFO, "Registration failed: username already exists");
                return new AuthResult(false, null, null, "Username already exists");
            }
            
            // Create new user profile
            String userId = "user_" + java.util.UUID.randomUUID().toString();
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            
            UserProfile newProfile = new UserProfile(
                userId,
                trimmedUsername,
                passwordHash,
                email,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            
            // Save user profile
            dataRepository.saveUserProfile(newProfile);
            
            // Generate JWT token for immediate login
            String token = generateToken(userId, trimmedUsername);
            
            LOGGER.log(Level.INFO, "User registration successful");
            return new AuthResult(true, token, userId, null);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Registration error for user: " + username, e);
            return new AuthResult(false, null, null, "Registration service error");
        }
    }
    
    /**
     * Utility method to hash a password using BCrypt
     * This can be used when creating new users
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
}
