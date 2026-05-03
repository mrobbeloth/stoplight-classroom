package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.Course;
import java.time.Instant;

public record CourseResponse(Long id, String name, String term, boolean archived, Instant createdAt) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getName(), c.getTerm(), c.isArchived(), c.getCreatedAt());
    }
}
