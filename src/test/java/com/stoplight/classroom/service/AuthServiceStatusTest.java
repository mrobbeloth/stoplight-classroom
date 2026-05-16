package com.stoplight.classroom.service;

import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.model.UserStatus;
import com.stoplight.classroom.repository.UserRepository;
import com.stoplight.classroom.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Targeted tests for the status-gating that {@link AuthService#login} performs. */
@ExtendWith(MockitoExtension.class)
class AuthServiceStatusTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_pendingUser_throws() {
        User user = new User("pending_teacher", "hashed", Role.TEACHER);
        user.setStatus(UserStatus.PENDING);
        when(userRepository.findByUsername("pending_teacher")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("pending_teacher", "pass123"));
        assertTrue(ex.getMessage().toLowerCase().contains("pending"));
    }

    @Test
    void login_rejectedUser_throws() {
        User user = new User("rejected_teacher", "hashed", Role.TEACHER);
        user.setStatus(UserStatus.REJECTED);
        when(userRepository.findByUsername("rejected_teacher")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("rejected_teacher", "pass123"));
        assertTrue(ex.getMessage().toLowerCase().contains("rejected"));
    }
}
