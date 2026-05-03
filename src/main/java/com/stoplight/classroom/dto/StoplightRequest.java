package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.StoplightValue;
import jakarta.validation.constraints.NotNull;

public record StoplightRequest(@NotNull StoplightValue value) {}
