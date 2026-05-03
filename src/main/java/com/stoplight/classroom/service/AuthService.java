package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.AuthResponse;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.UserRepository;
import com.stoplight.classroom.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken, "refresh")) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        var claims = jwtUtil.parseToken(refreshToken);
        String username = claims.getSubject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String newAccess = jwtUtil.generateAccessToken(user.getUsername(), user.getRole().name());
        String newRefresh = jwtUtil.generateRefreshToken(user.getUsername());
        return new AuthResponse(newAccess, newRefresh);
    }
}
