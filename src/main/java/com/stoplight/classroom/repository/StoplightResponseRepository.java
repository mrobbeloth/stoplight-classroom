package com.stoplight.classroom.repository;

import com.stoplight.classroom.model.StoplightResponse;
import com.stoplight.classroom.model.StoplightValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StoplightResponseRepository extends JpaRepository<StoplightResponse, Long> {
    Optional<StoplightResponse> findByParticipantIdAndSessionId(Long participantId, Long sessionId);
    List<StoplightResponse> findBySessionId(Long sessionId);

    @Query("SELECT r.value, COUNT(r) FROM StoplightResponse r WHERE r.session.id = :sessionId GROUP BY r.value")
    List<Object[]> countBySessionIdGroupByValue(Long sessionId);
}
