package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.AuthResponse;
import com.stoplight.classroom.dto.StudentLoginRequest;
import com.stoplight.classroom.dto.StudentRegisterRequest;
import com.stoplight.classroom.model.StudentAccount;
import com.stoplight.classroom.repository.StudentAccountRepository;
import com.stoplight.classroom.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentAccountService {

    private final StudentAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public StudentAccountService(StudentAccountRepository accountRepository,
                                  PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(StudentRegisterRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        StudentAccount account = new StudentAccount(
                request.email(), request.displayName(), passwordEncoder.encode(request.password()));
        accountRepository.save(account);
        return new AuthResponse(
                jwtUtil.generateAccessToken("sacct:" + account.getId(), "STUDENT_ACCOUNT"),
                jwtUtil.generateRefreshToken("sacct:" + account.getId()));
    }

    public AuthResponse login(StudentLoginRequest request) {
        StudentAccount account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new AuthResponse(
                jwtUtil.generateAccessToken("sacct:" + account.getId(), "STUDENT_ACCOUNT"),
                jwtUtil.generateRefreshToken("sacct:" + account.getId()));
    }

    public StudentAccount getById(Long id) {
        return accountRepository.findById(id).orElse(null);
    }
}
