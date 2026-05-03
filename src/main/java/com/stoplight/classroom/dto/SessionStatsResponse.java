package com.stoplight.classroom.dto;

import java.time.Instant;

public record SessionStatsResponse(
    Long sessionId,
    Long courseId,
    String courseName,
    long greenCount,
    long yellowCount,
    long redCount,
    long studentCount,
    Instant startedAt,
    Instant endedAt
) {}
