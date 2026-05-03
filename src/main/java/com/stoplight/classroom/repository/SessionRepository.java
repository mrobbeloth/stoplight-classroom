package com.stoplight.classroom.repository;

import com.stoplight.classroom.model.Session;
import com.stoplight.classroom.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByJoinCode(String joinCode);
    List<Session> findByCourseId(Long courseId);
    boolean existsByCourseTeacherIdAndStatus(Long teacherId, SessionStatus status);
    Optional<Session> findByCourseTeacherIdAndStatus(Long teacherId, SessionStatus status);
}
