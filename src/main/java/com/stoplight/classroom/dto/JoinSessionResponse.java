package com.stoplight.classroom.dto;

import com.stoplight.classroom.model.ActivityMode;

public record JoinSessionResponse(Long participantId, Long sessionId, String participantToken,
                                   ActivityMode activityMode) {}
