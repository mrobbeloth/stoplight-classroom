package com.stoplight.classroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public teacher signup request. The {@code .edu} restriction is applied in the service
 * layer (configurable via {@code stoplight.auth.allowed-email-suffixes}); the bean
 * validation here only checks general email/username/password shape.
 */
public record TeacherSignupRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 128) String password
) {}
