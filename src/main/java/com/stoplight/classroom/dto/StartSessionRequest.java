package com.stoplight.classroom.dto;

import jakarta.validation.constraints.NotNull;

public record StartSessionRequest(@NotNull Long courseId) {}
