package com.stoplight.classroom.repository;

import com.stoplight.classroom.model.SessionSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SessionSnapshotRepository extends JpaRepository<SessionSnapshot, Long> {
    Optional<SessionSnapshot> findBySessionId(Long sessionId);
    List<SessionSnapshot> findBySessionCourseId(Long courseId);
    List<SessionSnapshot> findBySessionCourseTeacherId(Long teacherId);
}
