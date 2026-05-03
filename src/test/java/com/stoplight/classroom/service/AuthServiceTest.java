package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.AuthResponse;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.UserRepository;
import com.stoplight.classroom.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_validCredentials_returnsTokens() {
        User user = new User("teacher1", "hashed", Role.TEACHER);
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);
        when(jwtUtil.generateAccessToken("teacher1", "TEACHER")).thenReturn("access-tok");
        when(jwtUtil.generateRefreshToken("teacher1")).thenReturn("refresh-tok");

        AuthResponse response = authService.login("teacher1", "pass123");

        assertEquals("access-tok", response.accessToken());
        assertEquals("refresh-tok", response.refreshToken());
    }

    @Test
    void login_invalidUsername_throws() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.login("unknown", "pass"));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User("teacher1", "hashed", Role.TEACHER);
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.login("teacher1", "wrong"));
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        when(jwtUtil.isValid("refresh-tok", "refresh")).thenReturn(true);
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("teacher1");
        when(jwtUtil.parseToken("refresh-tok")).thenReturn(claims);
        User user = new User("teacher1", "hashed", Role.TEACHER);
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("teacher1", "TEACHER")).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken("teacher1")).thenReturn("new-refresh");

        AuthResponse response = authService.refresh("refresh-tok");

        assertEquals("new-access", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
    }

    @Test
    void refresh_invalidToken_throws() {
        when(jwtUtil.isValid("bad-token", "refresh")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-token"));
    }
}
