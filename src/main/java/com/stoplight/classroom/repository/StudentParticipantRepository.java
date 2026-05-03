package com.stoplight.classroom.repository;

import com.stoplight.classroom.model.StudentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentParticipantRepository extends JpaRepository<StudentParticipant, Long> {
    List<StudentParticipant> findBySessionId(Long sessionId);
    long countBySessionId(Long sessionId);
}
