package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.*;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.*;
import com.stoplight.classroom.repository.*;
import com.stoplight.classroom.security.JwtUtil;
import com.stoplight.classroom.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StudentParticipantRepository participantRepository;
    private final CourseService courseService;
    private final StudentAccountService studentAccountService;
    private final JwtUtil jwtUtil;

    public SessionService(SessionRepository sessionRepository,
                          StudentParticipantRepository participantRepository,
                          CourseService courseService, StudentAccountService studentAccountService,
                          JwtUtil jwtUtil) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.courseService = courseService;
        this.studentAccountService = studentAccountService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public SessionResponse startSession(String teacherUsername, StartSessionRequest request) {
        if (sessionRepository.existsByCourseTeacherIdAndStatus(
                courseService.findOwned(teacherUsername, request.courseId()).getTeacher().getId(),
                SessionStatus.ACTIVE)) {
            throw new IllegalArgumentException("Teacher already has an active session");
        }
        Course course = courseService.findOwned(teacherUsername, request.courseId());
        String joinCode = generateUniqueCode();
        Session session = new Session(course, joinCode);
        return SessionResponse.from(sessionRepository.save(session));
    }

    @Transactional
    public SessionResponse endSession(String teacherUsername, Long sessionId) {
        Session session = findOwnedSession(teacherUsername, sessionId);
        if (session.getStatus() == SessionStatus.ENDED) {
            throw new IllegalArgumentException("Session already ended");
        }
        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        return SessionResponse.from(sessionRepository.save(session));
    }

    @Transactional
    public SessionResponse setActivityMode(String teacherUsername, Long sessionId, ActivityMode mode) {
        Session session = findOwnedActiveSession(teacherUsername, sessionId);
        session.setActivityMode(mode);
        return SessionResponse.from(sessionRepository.save(session));
    }

    @Transactional
    public JoinSessionResponse joinSession(JoinSessionRequest request, Long studentAccountId) {
        Session session = sessionRepository.findByJoinCode(request.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid join code"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Session is not active");
        }
        StudentParticipant participant = new StudentParticipant(session, request.displayName());
        if (studentAccountId != null && studentAccountService != null) {
            var account = studentAccountService.getById(studentAccountId);
            if (account != null) participant.setStudentAccount(account);
        }
        participantRepository.save(participant);
        String token = jwtUtil.generateAccessToken(
                "student:" + participant.getId(), "STUDENT");
        return new JoinSessionResponse(participant.getId(), session.getId(), token,
                session.getActivityMode());
    }

    public SessionResponse getSession(Long sessionId) {
        return SessionResponse.from(sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found")));
    }

    Session findOwnedActiveSession(String teacherUsername, Long sessionId) {
        Session session = findOwnedSession(teacherUsername, sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Session is not active");
        }
        return session;
    }

    private Session findOwnedSession(String teacherUsername, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        courseService.findOwned(teacherUsername, session.getCourse().getId());
        return session;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = CodeGenerator.generate(6);
            if (sessionRepository.findByJoinCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique join code");
    }
}
