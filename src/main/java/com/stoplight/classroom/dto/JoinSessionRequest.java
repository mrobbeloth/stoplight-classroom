package com.stoplight.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinSessionRequest(@NotBlank String joinCode, @NotBlank String displayName) {}
