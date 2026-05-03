package com.stoplight.classroom.dto;

public record LifetimeStatsResponse(
    int courseCount,
    int sessionCount,
    long totalGreen,
    long totalYellow,
    long totalRed,
    long totalStudentResponses
) {}
