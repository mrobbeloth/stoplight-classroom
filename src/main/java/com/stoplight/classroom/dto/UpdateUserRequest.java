package com.stoplight.classroom.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(min = 8, max = 128) String password
) {}
