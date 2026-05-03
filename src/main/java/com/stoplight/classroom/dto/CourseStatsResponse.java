package com.stoplight.classroom.dto;

import java.util.List;

public record CourseStatsResponse(
    Long courseId,
    String courseName,
    String term,
    int sessionCount,
    long totalGreen,
    long totalYellow,
    long totalRed,
    long totalStudentResponses,
    List<SessionStatsResponse> sessions
) {}
