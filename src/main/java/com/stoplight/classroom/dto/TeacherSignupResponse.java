package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.User;
import com.stoplight.classroom.model.UserStatus;

import java.time.Instant;

/**
 * Response returned from the public teacher signup endpoint, and the shape used by the
 * admin pending-list endpoint. Deliberately omits the password hash.
 */
public record TeacherSignupResponse(
        Long id,
        String username,
        String email,
        UserStatus status,
        Instant createdAt
) {
    public static TeacherSignupResponse from(User user) {
        return new TeacherSignupResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getStatus(), user.getCreatedAt());
    }
}
