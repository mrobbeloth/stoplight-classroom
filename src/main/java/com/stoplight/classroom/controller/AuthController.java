package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.AuthResponse;
import com.stoplight.classroom.dto.LoginRequest;
import com.stoplight.classroom.dto.RefreshRequest;
import com.stoplight.classroom.dto.TeacherSignupRequest;
import com.stoplight.classroom.dto.TeacherSignupResponse;
import com.stoplight.classroom.service.AuthService;
import com.stoplight.classroom.service.TeacherSignupProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TeacherSignupProvider teacherSignupProvider;

    public AuthController(AuthService authService, TeacherSignupProvider teacherSignupProvider) {
        this.authService = authService;
        this.teacherSignupProvider = teacherSignupProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Public teacher signup. Creates a PENDING account that an admin must approve before
     * the user can log in. Returns 201 with the pending record (no tokens are issued).
     */
    @PostMapping("/teacher/signup")
    public ResponseEntity<TeacherSignupResponse> signup(@Valid @RequestBody TeacherSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teacherSignupProvider.requestSignup(request));
    }
}
