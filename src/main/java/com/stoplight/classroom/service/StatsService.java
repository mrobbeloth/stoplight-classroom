package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.*;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.*;
import com.stoplight.classroom.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatsService {

    private final SessionSnapshotRepository snapshotRepository;
    private final SessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final StudentParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final StoplightService stoplightService;

    public StatsService(SessionSnapshotRepository snapshotRepository, SessionRepository sessionRepository,
                        CourseRepository courseRepository, StudentParticipantRepository participantRepository,
                        UserRepository userRepository, StoplightService stoplightService) {
        this.snapshotRepository = snapshotRepository;
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.stoplightService = stoplightService;
    }

    @Transactional
    public void captureSnapshot(Session session) {
        if (snapshotRepository.findBySessionId(session.getId()).isPresent()) return;
        StoplightAggregate agg = stoplightService.getAggregate(session.getId());
        long studentCount = participantRepository.countBySessionId(session.getId());
        snapshotRepository.save(new SessionSnapshot(session, agg.green(), agg.yellow(), agg.red(), studentCount));
    }

    @Transactional
    public void captureSnapshotById(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        captureSnapshot(session);
    }

    public SessionStatsResponse getSessionStats(String teacherUsername, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        verifyOwnership(teacherUsername, session.getCourse());
        return buildSessionStats(session);
    }

    public CourseStatsResponse getCourseStats(String teacherUsername, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        verifyOwnership(teacherUsername, course);

        List<SessionStatsResponse> sessions = sessionRepository.findByCourseId(courseId).stream()
                .filter(s -> s.getStatus() == SessionStatus.ENDED)
                .map(this::buildSessionStats)
                .toList();

        long totalG = sessions.stream().mapToLong(SessionStatsResponse::greenCount).sum();
        long totalY = sessions.stream().mapToLong(SessionStatsResponse::yellowCount).sum();
        long totalR = sessions.stream().mapToLong(SessionStatsResponse::redCount).sum();

        return new CourseStatsResponse(courseId, course.getName(), course.getTerm(),
                sessions.size(), totalG, totalY, totalR, totalG + totalY + totalR, sessions);
    }

    public LifetimeStatsResponse getLifetimeStats(String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<SessionSnapshot> snapshots = snapshotRepository.findBySessionCourseTeacherId(teacher.getId());
        int courseCount = courseRepository.findByTeacherId(teacher.getId()).size();

        long totalG = snapshots.stream().mapToLong(SessionSnapshot::getGreenCount).sum();
        long totalY = snapshots.stream().mapToLong(SessionSnapshot::getYellowCount).sum();
        long totalR = snapshots.stream().mapToLong(SessionSnapshot::getRedCount).sum();

        return new LifetimeStatsResponse(courseCount, snapshots.size(), totalG, totalY, totalR,
                totalG + totalY + totalR);
    }

    private SessionStatsResponse buildSessionStats(Session session) {
        var snapshot = snapshotRepository.findBySessionId(session.getId());
        long g = 0, y = 0, r = 0, students = 0;
        if (snapshot.isPresent()) {
            g = snapshot.get().getGreenCount();
            y = snapshot.get().getYellowCount();
            r = snapshot.get().getRedCount();
            students = snapshot.get().getStudentCount();
        } else {
            StoplightAggregate agg = stoplightService.getAggregate(session.getId());
            g = agg.green(); y = agg.yellow(); r = agg.red();
            students = participantRepository.countBySessionId(session.getId());
        }
        return new SessionStatsResponse(session.getId(), session.getCourse().getId(),
                session.getCourse().getName(), g, y, r, students,
                session.getStartedAt(), session.getEndedAt());
    }

    private void verifyOwnership(String teacherUsername, Course course) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new ResourceNotFoundException("Course not found");
        }
    }
}
