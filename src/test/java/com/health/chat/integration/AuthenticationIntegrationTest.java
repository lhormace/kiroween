package com.health.chat.integration;

import com.health.chat.model.AuthResult;
import com.health.chat.model.UserProfile;
import com.health.chat.repository.DataRepository;
import com.health.chat.service.JwtAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("認証統合テスト")
class AuthenticationIntegrationTest {

    private DataRepository mockRepository;
    private JwtAuthenticationService authService;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-minimum-32-chars";

    @BeforeEach
    void setUp() {
        mockRepository = mock(DataRepository.class);
        authService = new JwtAuthenticationService(mockRepository, TEST_SECRET);
    }

    @Test
    @DisplayName("完全な登録→ログイン→トークン検証フロー")
    void testCompleteAuthenticationFlow() {
        // Step 1: Register a new user
        String username = "integrationuser";
        String password = "securePassword123";
        String email = "integration@example.com";
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(null);
        
        AuthResult registerResult = authService.registerUser(username, password, email);
        
        assertTrue(registerResult.isSuccess(), "登録は成功すべき");
        assertNotNull(registerResult.getToken(), "登録時にトークンが発行されるべき");
        String registrationToken = registerResult.getToken();
        String userId = registerResult.getUserId();
        
        // Verify token is valid
        assertTrue(authService.validateToken(registrationToken), "登録トークンは有効であるべき");
        
        // Verify userId can be extracted from token
        assertEquals(userId, authService.getUserIdFromToken(registrationToken), 
                    "トークンから正しいユーザーIDを取得できるべき");
        
        // Step 2: Simulate user profile being saved and retrieved
        String passwordHash = registerResult.getToken(); // In real scenario, this would be the saved hash
        UserProfile savedProfile = new UserProfile(
            userId,
            username,
            JwtAuthenticationService.hashPassword(password),
            email,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(savedProfile);
        
        // Step 3: Login with the same credentials
        AuthResult loginResult = authService.authenticate(username, password);
        
        assertTrue(loginResult.isSuccess(), "ログインは成功すべき");
        assertNotNull(loginResult.getToken(), "ログイン時にトークンが発行されるべき");
        assertEquals(userId, loginResult.getUserId(), "同じユーザーIDが返されるべき");
        
        String loginToken = loginResult.getToken();
        
        // Step 4: Validate login token
        assertTrue(authService.validateToken(loginToken), "ログイントークンは有効であるべき");
        
        // Step 5: Extract userId from login token
        assertEquals(userId, authService.getUserIdFromToken(loginToken), 
                    "ログイントークンから正しいユーザーIDを取得できるべき");
        
        // Step 6: Logout (invalidate token)
        authService.invalidateToken(loginToken);
        assertFalse(authService.validateToken(loginToken), 
                   "無効化されたトークンは検証を通過しないべき");
    }

    @Test
    @DisplayName("複数ユーザーの登録と認証")
    void testMultipleUserRegistrationAndAuthentication() {
        // Register first user
        String user1 = "user1";
        String pass1 = "password1";
        String email1 = "user1@example.com";
        
        when(mockRepository.getUserProfileByUsername(user1)).thenReturn(null);
        AuthResult result1 = authService.registerUser(user1, pass1, email1);
        assertTrue(result1.isSuccess());
        
        // Register second user
        String user2 = "user2";
        String pass2 = "password2";
        String email2 = "user2@example.com";
        
        when(mockRepository.getUserProfileByUsername(user2)).thenReturn(null);
        AuthResult result2 = authService.registerUser(user2, pass2, email2);
        assertTrue(result2.isSuccess());
        
        // Verify different user IDs
        assertNotEquals(result1.getUserId(), result2.getUserId(), 
                       "異なるユーザーには異なるIDが割り当てられるべき");
        
        // Verify different tokens
        assertNotEquals(result1.getToken(), result2.getToken(), 
                       "異なるユーザーには異なるトークンが発行されるべき");
        
        // Both tokens should be valid
        assertTrue(authService.validateToken(result1.getToken()));
        assertTrue(authService.validateToken(result2.getToken()));
        
        // Verify correct userId extraction
        assertEquals(result1.getUserId(), authService.getUserIdFromToken(result1.getToken()));
        assertEquals(result2.getUserId(), authService.getUserIdFromToken(result2.getToken()));
    }

    @Test
    @DisplayName("セキュリティ：パスワード再利用攻撃の防止")
    void testPasswordReusePrevention() {
        String username = "securitytest";
        String password = "commonPassword123";
        String email = "security@example.com";
        
        // Register user
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(null);
        AuthResult registerResult = authService.registerUser(username, password, email);
        assertTrue(registerResult.isSuccess());
        
        // Simulate saved profile
        UserProfile profile = new UserProfile(
            registerResult.getUserId(),
            username,
            JwtAuthenticationService.hashPassword(password),
            email,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);
        
        // Try to authenticate with wrong password
        AuthResult wrongPasswordResult = authService.authenticate(username, "wrongPassword");
        assertFalse(wrongPasswordResult.isSuccess(), "間違ったパスワードでは認証失敗すべき");
        
        // Authenticate with correct password
        AuthResult correctPasswordResult = authService.authenticate(username, password);
        assertTrue(correctPasswordResult.isSuccess(), "正しいパスワードでは認証成功すべき");
    }

    @Test
    @DisplayName("トークン無効化の動作確認")
    void testTokenInvalidation() {
        String username = "tokentest";
        String password = "password123";
        String email = "token@example.com";
        
        // Register and get token
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(null);
        AuthResult registerResult = authService.registerUser(username, password, email);
        String token = registerResult.getToken();
        
        // Verify token is valid
        assertTrue(authService.validateToken(token), "登録トークンは有効であるべき");
        
        // Invalidate token
        authService.invalidateToken(token);
        
        // Token should now be invalid
        assertFalse(authService.validateToken(token), "無効化されたトークンは無効であるべき");
        
        // Wait 1 second to ensure different timestamp for new token
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Verify we can still authenticate and get a new token
        UserProfile profile = new UserProfile(
            registerResult.getUserId(),
            username,
            JwtAuthenticationService.hashPassword(password),
            email,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);
        
        AuthResult loginResult = authService.authenticate(username, password);
        assertTrue(loginResult.isSuccess(), "再ログインは成功すべき");
        assertNotNull(loginResult.getToken(), "新しいトークンが発行されるべき");
        assertNotEquals(token, loginResult.getToken(), "異なるトークンが生成されるべき");
    }
}
