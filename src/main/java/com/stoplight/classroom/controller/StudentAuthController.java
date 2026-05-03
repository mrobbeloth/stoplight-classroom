package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.AuthResponse;
import com.stoplight.classroom.dto.StudentLoginRequest;
import com.stoplight.classroom.dto.StudentRegisterRequest;
import com.stoplight.classroom.service.StudentAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/auth")
public class StudentAuthController {

    private final StudentAccountService studentAccountService;

    public StudentAuthController(StudentAccountService studentAccountService) {
        this.studentAccountService = studentAccountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentAccountService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ResponseEntity.ok(studentAccountService.login(request));
    }
}
