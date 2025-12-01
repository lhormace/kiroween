package com.health.chat.service;

import com.health.chat.model.AuthResult;
import com.health.chat.model.UserProfile;
import com.health.chat.repository.DataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JWT認証サービステスト")
class JwtAuthenticationServiceTest {

    private DataRepository mockRepository;
    private JwtAuthenticationService authService;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-minimum-32-chars";

    @BeforeEach
    void setUp() {
        mockRepository = mock(DataRepository.class);
        authService = new JwtAuthenticationService(mockRepository, TEST_SECRET);
    }

    @Test
    @DisplayName("正常なユーザー登録")
    void testSuccessfulRegistration() {
        // Given
        String username = "newuser";
        String password = "password123";
        String email = "newuser@example.com";
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(null);

        // When
        AuthResult result = authService.registerUser(username, password, email);

        // Then
        assertTrue(result.isSuccess(), "登録は成功すべき");
        assertNotNull(result.getToken(), "トークンが生成されるべき");
        assertNotNull(result.getUserId(), "ユーザーIDが生成されるべき");
        assertNull(result.getErrorMessage(), "エラーメッセージはnullであるべき");
        
        // Verify user profile was saved
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(mockRepository).saveUserProfile(profileCaptor.capture());
        
        UserProfile savedProfile = profileCaptor.getValue();
        assertEquals(username, savedProfile.getUsername());
        assertEquals(email, savedProfile.getEmail());
        assertNotNull(savedProfile.getPasswordHash());
        assertNotEquals(password, savedProfile.getPasswordHash(), "パスワードはハッシュ化されるべき");
    }

    @Test
    @DisplayName("重複ユーザー名での登録失敗")
    void testRegistrationWithDuplicateUsername() {
        // Given
        String username = "existinguser";
        UserProfile existingProfile = new UserProfile(
            "user_123",
            username,
            "hashedpassword",
            "existing@example.com",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(existingProfile);

        // When
        AuthResult result = authService.registerUser(username, "password123", "new@example.com");

        // Then
        assertFalse(result.isSuccess(), "登録は失敗すべき");
        assertNull(result.getToken());
        assertNull(result.getUserId());
        assertEquals("Username already exists", result.getErrorMessage());
        
        verify(mockRepository, never()).saveUserProfile(any());
    }

    @Test
    @DisplayName("無効な入力での登録失敗")
    void testRegistrationWithInvalidInput() {
        // Empty username
        AuthResult result1 = authService.registerUser("", "password123", "test@example.com");
        assertFalse(result1.isSuccess());
        assertEquals("Username cannot be empty", result1.getErrorMessage());

        // Short password
        AuthResult result2 = authService.registerUser("user", "short", "test@example.com");
        assertFalse(result2.isSuccess());
        assertEquals("Password must be at least 8 characters", result2.getErrorMessage());

        // Invalid email
        AuthResult result3 = authService.registerUser("user", "password123", "invalid-email");
        assertFalse(result3.isSuccess());
        assertEquals("Invalid email address", result3.getErrorMessage());
        
        verify(mockRepository, never()).saveUserProfile(any());
    }

    @Test
    @DisplayName("正常なログイン認証")
    void testSuccessfulAuthentication() {
        // Given
        String username = "testuser";
        String password = "password123";
        String passwordHash = JwtAuthenticationService.hashPassword(password);
        
        UserProfile profile = new UserProfile(
            "user_123",
            username,
            passwordHash,
            "test@example.com",
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().minusDays(1)
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);

        // When
        AuthResult result = authService.authenticate(username, password);

        // Then
        assertTrue(result.isSuccess(), "認証は成功すべき");
        assertNotNull(result.getToken(), "トークンが生成されるべき");
        assertEquals("user_123", result.getUserId());
        assertNull(result.getErrorMessage());
        
        // Verify last login was updated
        verify(mockRepository).saveUserProfile(any(UserProfile.class));
    }

    @Test
    @DisplayName("存在しないユーザーでの認証失敗")
    void testAuthenticationWithNonExistentUser() {
        // Given
        when(mockRepository.getUserProfileByUsername("nonexistent")).thenReturn(null);

        // When
        AuthResult result = authService.authenticate("nonexistent", "password");

        // Then
        assertFalse(result.isSuccess());
        assertNull(result.getToken());
        assertNull(result.getUserId());
        assertEquals("Invalid username or password", result.getErrorMessage());
    }

    @Test
    @DisplayName("間違ったパスワードでの認証失敗")
    void testAuthenticationWithWrongPassword() {
        // Given
        String username = "testuser";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String passwordHash = JwtAuthenticationService.hashPassword(correctPassword);
        
        UserProfile profile = new UserProfile(
            "user_123",
            username,
            passwordHash,
            "test@example.com",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);

        // When
        AuthResult result = authService.authenticate(username, wrongPassword);

        // Then
        assertFalse(result.isSuccess());
        assertNull(result.getToken());
        assertNull(result.getUserId());
        assertEquals("Invalid username or password", result.getErrorMessage());
    }

    @Test
    @DisplayName("有効なトークンの検証")
    void testValidTokenValidation() {
        // Given - Create a valid token through authentication
        String username = "testuser";
        String password = "password123";
        String passwordHash = JwtAuthenticationService.hashPassword(password);
        
        UserProfile profile = new UserProfile(
            "user_123",
            username,
            passwordHash,
            "test@example.com",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);
        AuthResult authResult = authService.authenticate(username, password);
        String token = authResult.getToken();

        // When
        boolean isValid = authService.validateToken(token);

        // Then
        assertTrue(isValid, "有効なトークンは検証を通過すべき");
    }

    @Test
    @DisplayName("無効なトークンの検証失敗")
    void testInvalidTokenValidation() {
        // When & Then
        assertFalse(authService.validateToken(null), "nullトークンは無効");
        assertFalse(authService.validateToken(""), "空トークンは無効");
        assertFalse(authService.validateToken("invalid-token"), "不正なトークンは無効");
    }

    @Test
    @DisplayName("トークンからユーザーID取得")
    void testGetUserIdFromToken() {
        // Given
        String username = "testuser";
        String password = "password123";
        String passwordHash = JwtAuthenticationService.hashPassword(password);
        
        UserProfile profile = new UserProfile(
            "user_123",
            username,
            passwordHash,
            "test@example.com",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);
        AuthResult authResult = authService.authenticate(username, password);
        String token = authResult.getToken();

        // When
        String userId = authService.getUserIdFromToken(token);

        // Then
        assertEquals("user_123", userId, "トークンから正しいユーザーIDを取得できるべき");
    }

    @Test
    @DisplayName("無効化されたトークンの検証失敗")
    void testInvalidatedTokenValidation() {
        // Given
        String username = "testuser";
        String password = "password123";
        String passwordHash = JwtAuthenticationService.hashPassword(password);
        
        UserProfile profile = new UserProfile(
            "user_123",
            username,
            passwordHash,
            "test@example.com",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(mockRepository.getUserProfileByUsername(username)).thenReturn(profile);
        AuthResult authResult = authService.authenticate(username, password);
        String token = authResult.getToken();

        // When
        authService.invalidateToken(token);
        boolean isValid = authService.validateToken(token);

        // Then
        assertFalse(isValid, "無効化されたトークンは検証を通過しないべき");
    }

    @Test
    @DisplayName("パスワードハッシュ化の検証")
    void testPasswordHashing() {
        // Given
        String plainPassword = "mySecurePassword123";

        // When
        String hash1 = JwtAuthenticationService.hashPassword(plainPassword);
        String hash2 = JwtAuthenticationService.hashPassword(plainPassword);

        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(plainPassword, hash1, "ハッシュは平文と異なるべき");
        assertNotEquals(hash1, hash2, "同じパスワードでも異なるハッシュが生成されるべき（salt使用）");
        
        // Verify BCrypt can verify both hashes
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, hash1));
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, hash2));
    }
}
