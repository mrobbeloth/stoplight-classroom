package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.*;
import com.stoplight.classroom.exception.ActiveSessionExistsException;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.*;
import com.stoplight.classroom.repository.*;
import com.stoplight.classroom.security.JwtUtil;
import com.stoplight.classroom.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StudentParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final StudentAccountService studentAccountService;
    private final JwtUtil jwtUtil;

    public SessionService(SessionRepository sessionRepository,
                          StudentParticipantRepository participantRepository,
                          UserRepository userRepository,
                          CourseService courseService, StudentAccountService studentAccountService,
                          JwtUtil jwtUtil) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.studentAccountService = studentAccountService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public SessionResponse startSession(String teacherUsername, StartSessionRequest request) {
        Course course = courseService.findOwned(teacherUsername, request.courseId());
        Long teacherId = course.getTeacher().getId();

        Optional<Session> existing = sessionRepository
                .findByCourseTeacherIdAndStatus(teacherId, SessionStatus.ACTIVE);
        if (existing.isPresent()) {
            throw new ActiveSessionExistsException(SessionResponse.from(existing.get()));
        }

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

    /**
     * Returns the calling teacher's currently ACTIVE session, if any.
     * Used by the dashboard to surface a Resume affordance after navigation away.
     */
    public Optional<SessionResponse> getActiveSessionForTeacher(String teacherUsername) {
        User teacher = findTeacher(teacherUsername);
        return sessionRepository
                .findByCourseTeacherIdAndStatus(teacher.getId(), SessionStatus.ACTIVE)
                .map(SessionResponse::from);
    }

    /**
     * Returns the teacher's sessions for a course, newest first. Throws if the teacher
     * does not own the course.
     */
    public List<SessionResponse> listSessionsForCourse(String teacherUsername, Long courseId) {
        // Ownership / 404 check.
        courseService.findOwned(teacherUsername, courseId);
        return sessionRepository.findByCourseIdOrderByStartedAtDesc(courseId).stream()
                .map(SessionResponse::from).toList();
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

    private User findTeacher(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
