package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.ActivityMode;
import jakarta.validation.constraints.NotNull;

public record ActivityModeRequest(@NotNull ActivityMode activityMode) {}
