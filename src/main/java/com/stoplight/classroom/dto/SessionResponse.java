package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.ActivityMode;
import com.stoplight.classroom.model.Session;
import com.stoplight.classroom.model.SessionStatus;
import java.time.Instant;

public record SessionResponse(Long id, Long courseId, String joinCode, ActivityMode activityMode,
                               SessionStatus status, Instant startedAt, Instant endedAt) {
    public static SessionResponse from(Session s) {
        return new SessionResponse(s.getId(), s.getCourse().getId(), s.getJoinCode(),
                s.getActivityMode(), s.getStatus(), s.getStartedAt(), s.getEndedAt());
    }
}
