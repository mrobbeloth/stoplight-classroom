package com.stoplight.classroom.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
    @NotBlank String name,
    String term
) {}
