package ru.netology.cloudservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import ru.netology.cloudservice.dto.LoginRequest;
import ru.netology.cloudservice.entity.AuthTokenEntity;
import ru.netology.cloudservice.entity.UserEntity;
import ru.netology.cloudservice.repository.AuthTokenRepository;
import ru.netology.cloudservice.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity testUser;
    private LoginRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setLogin("user");
        testUser.setPasswordHash("$2a$10$hashed");

        validRequest = new LoginRequest();
        validRequest.setLogin("user");
        validRequest.setPassword("password");
    }

    @Test
    void login_success_returnsToken() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", testUser.getPasswordHash())).thenReturn(true);
        when(authTokenRepository.save(any(AuthTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.login(validRequest);

        assertNotNull(token);
        verify(authTokenRepository).save(any(AuthTokenEntity.class));
    }

    @Test
    void login_userNotFound_throwsException() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(validRequest));
        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(userRepository.findByLogin("user")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", testUser.getPasswordHash())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(validRequest));
        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void logout_revokesToken() {
        AuthTokenEntity tokenEntity = new AuthTokenEntity();
        tokenEntity.setToken("abc-123");
        tokenEntity.setRevoked(false);
        when(authTokenRepository.findByTokenAndRevokedFalse("abc-123")).thenReturn(Optional.of(tokenEntity));
        when(authTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout("abc-123");

        assertTrue(tokenEntity.isRevoked());
        verify(authTokenRepository).save(tokenEntity);
    }

    @Test
    void logout_stripsBearerPrefix() {
        AuthTokenEntity tokenEntity = new AuthTokenEntity();
        tokenEntity.setToken("xyz-456");
        tokenEntity.setRevoked(false);
        when(authTokenRepository.findByTokenAndRevokedFalse("xyz-456")).thenReturn(Optional.of(tokenEntity));
        when(authTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout("Bearer xyz-456");

        verify(authTokenRepository).findByTokenAndRevokedFalse("xyz-456");
    }

    @Test
    void authenticateByToken_validToken_returnsAuthentication() {
        AuthTokenEntity tokenEntity = new AuthTokenEntity();
        tokenEntity.setToken("valid-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiresAt(Instant.now().plusSeconds(3600));
        when(authTokenRepository.findByTokenAndRevokedFalse("valid-token")).thenReturn(Optional.of(tokenEntity));

        Authentication auth = authService.authenticateByToken("valid-token");

        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
    }

    @Test
    void authenticateByToken_invalidToken_returnsNull() {
        when(authTokenRepository.findByTokenAndRevokedFalse("invalid")).thenReturn(Optional.empty());

        Authentication auth = authService.authenticateByToken("invalid");

        assertNull(auth);
    }

    @Test
    void authenticateByToken_blankToken_returnsNull() {
        Authentication auth = authService.authenticateByToken("   ");
        assertNull(auth);
    }
}
