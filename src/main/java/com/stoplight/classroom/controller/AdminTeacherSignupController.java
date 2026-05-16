package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.TeacherSignupResponse;
import com.stoplight.classroom.model.UserStatus;
import com.stoplight.classroom.service.TeacherSignupProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for reviewing public teacher signups. Mounted under {@code /api/admin/}
 * so it is automatically gated by the {@code hasRole("ADMIN")} rule in
 * {@link com.stoplight.classroom.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/teacher-signups")
public class AdminTeacherSignupController {

    private final TeacherSignupProvider provider;

    public AdminTeacherSignupController(TeacherSignupProvider provider) {
        this.provider = provider;
    }

    /** List teacher signups by status. Defaults to {@code PENDING} for the review queue. */
    @GetMapping
    public ResponseEntity<List<TeacherSignupResponse>> list(
            @RequestParam(name = "status", defaultValue = "PENDING") UserStatus status) {
        return ResponseEntity.ok(provider.listByStatus(status));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TeacherSignupResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(provider.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TeacherSignupResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(provider.reject(id));
    }
}
