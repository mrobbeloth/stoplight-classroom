package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import java.time.Instant;

public record UserResponse(Long id, String username, Role role, Instant createdAt, Instant updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
