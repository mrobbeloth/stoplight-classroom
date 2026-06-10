package com.stoplight.classroom.exception;

import com.stoplight.classroom.dto.SessionResponse;

/**
 * Thrown when a teacher tries to start a new session while one of theirs is still ACTIVE.
 * Carries the existing {@link SessionResponse} so callers can offer a "resume" path.
 */
public class ActiveSessionExistsException extends RuntimeException {

    private final SessionResponse session;

    public ActiveSessionExistsException(SessionResponse session) {
        super("Teacher already has an active session");
        this.session = session;
    }

    public SessionResponse getSession() {
        return session;
    }
}
