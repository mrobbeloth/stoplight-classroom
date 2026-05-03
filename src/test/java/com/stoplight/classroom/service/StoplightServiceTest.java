package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.StoplightAggregate;
import com.stoplight.classroom.dto.StoplightRequest;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.*;
import com.stoplight.classroom.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoplightServiceTest {

    @Mock private StoplightResponseRepository responseRepository;
    @Mock private StudentParticipantRepository participantRepository;
    @Mock private SessionRepository sessionRepository;

    private StoplightService stoplightService;

    @BeforeEach
    void setUp() {
        stoplightService = new StoplightService(responseRepository, participantRepository, sessionRepository);
    }

    private Session activeSession() {
        Session s = mock(Session.class);
        lenient().when(s.getId()).thenReturn(1L);
        lenient().when(s.getStatus()).thenReturn(SessionStatus.ACTIVE);
        return s;
    }

    private StudentParticipant participantInSession(Session session) {
        StudentParticipant p = mock(StudentParticipant.class);
        lenient().when(p.getId()).thenReturn(10L);
        lenient().when(p.getSession()).thenReturn(session);
        return p;
    }

    @Test
    void submitResponse_newResponse_creates() {
        Session session = activeSession();
        StudentParticipant participant = participantInSession(session);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(participantRepository.findById(10L)).thenReturn(Optional.of(participant));
        when(responseRepository.findByParticipantIdAndSessionId(10L, 1L)).thenReturn(Optional.empty());

        stoplightService.submitResponse(10L, 1L, new StoplightRequest(StoplightValue.GREEN));

        verify(responseRepository).save(any(StoplightResponse.class));
    }

    @Test
    void submitResponse_existingResponse_updates() {
        Session session = activeSession();
        StudentParticipant participant = participantInSession(session);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(participantRepository.findById(10L)).thenReturn(Optional.of(participant));
        StoplightResponse existing = new StoplightResponse(participant, session, StoplightValue.GREEN);
        when(responseRepository.findByParticipantIdAndSessionId(10L, 1L)).thenReturn(Optional.of(existing));

        stoplightService.submitResponse(10L, 1L, new StoplightRequest(StoplightValue.RED));

        assertEquals(StoplightValue.RED, existing.getValue());
        verify(responseRepository).save(existing);
    }

    @Test
    void submitResponse_endedSession_throws() {
        Session ended = mock(Session.class);
        when(ended.getStatus()).thenReturn(SessionStatus.ENDED);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(ended));

        assertThrows(IllegalArgumentException.class,
                () -> stoplightService.submitResponse(10L, 1L, new StoplightRequest(StoplightValue.GREEN)));
    }

    @Test
    void submitResponse_wrongSession_throws() {
        Session session = activeSession();
        Session otherSession = mock(Session.class);
        when(otherSession.getId()).thenReturn(99L);
        StudentParticipant wrongParticipant = mock(StudentParticipant.class);
        when(wrongParticipant.getSession()).thenReturn(otherSession);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(participantRepository.findById(10L)).thenReturn(Optional.of(wrongParticipant));

        assertThrows(IllegalArgumentException.class,
                () -> stoplightService.submitResponse(10L, 1L, new StoplightRequest(StoplightValue.GREEN)));
    }

    @Test
    void getAggregate_returnsCorrectCounts() {
        when(responseRepository.countBySessionIdGroupByValue(1L)).thenReturn(List.of(
                new Object[]{StoplightValue.GREEN, 5L},
                new Object[]{StoplightValue.YELLOW, 3L},
                new Object[]{StoplightValue.RED, 2L}
        ));

        StoplightAggregate agg = stoplightService.getAggregate(1L);

        assertEquals(5, agg.green());
        assertEquals(3, agg.yellow());
        assertEquals(2, agg.red());
        assertEquals(10, agg.total());
    }
}
