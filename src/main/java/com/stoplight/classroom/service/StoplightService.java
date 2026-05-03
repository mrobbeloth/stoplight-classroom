package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.StoplightAggregate;
import com.stoplight.classroom.dto.StoplightRequest;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.*;
import com.stoplight.classroom.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoplightService {

    private final StoplightResponseRepository responseRepository;
    private final StudentParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;

    public StoplightService(StoplightResponseRepository responseRepository,
                            StudentParticipantRepository participantRepository,
                            SessionRepository sessionRepository) {
        this.responseRepository = responseRepository;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public void submitResponse(Long participantId, Long sessionId, StoplightRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("Session is not active");
        }
        StudentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        if (!participant.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Participant not in this session");
        }

        var existing = responseRepository.findByParticipantIdAndSessionId(participantId, sessionId);
        if (existing.isPresent()) {
            existing.get().setValue(request.value());
            responseRepository.save(existing.get());
        } else {
            responseRepository.save(new StoplightResponse(participant, session, request.value()));
        }
    }

    public StoplightAggregate getAggregate(Long sessionId) {
        var rows = responseRepository.countBySessionIdGroupByValue(sessionId);
        long green = 0, yellow = 0, red = 0;
        for (Object[] row : rows) {
            StoplightValue val = (StoplightValue) row[0];
            long count = (Long) row[1];
            switch (val) {
                case GREEN -> green = count;
                case YELLOW -> yellow = count;
                case RED -> red = count;
            }
        }
        return new StoplightAggregate(green, yellow, red, green + yellow + red);
    }
}
