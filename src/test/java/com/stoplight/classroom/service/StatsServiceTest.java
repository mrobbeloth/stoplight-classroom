package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.LifetimeStatsResponse;
import com.stoplight.classroom.dto.StoplightAggregate;
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
class StatsServiceTest {

    @Mock private SessionSnapshotRepository snapshotRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private StudentParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoplightService stoplightService;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(snapshotRepository, sessionRepository, courseRepository,
                participantRepository, userRepository, stoplightService);
    }

    @Test
    void captureSnapshot_savesAggregate() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);
        when(snapshotRepository.findBySessionId(1L)).thenReturn(Optional.empty());
        when(stoplightService.getAggregate(1L)).thenReturn(new StoplightAggregate(5, 3, 2, 10));
        when(participantRepository.countBySessionId(1L)).thenReturn(10L);

        statsService.captureSnapshot(session);

        verify(snapshotRepository).save(any(SessionSnapshot.class));
    }

    @Test
    void captureSnapshot_skipsIfAlreadyExists() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);
        when(snapshotRepository.findBySessionId(1L)).thenReturn(Optional.of(mock(SessionSnapshot.class)));

        statsService.captureSnapshot(session);

        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void getLifetimeStats_aggregatesSnapshots() {
        User teacher = new User("t1", "h", Role.TEACHER);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(teacher, 1L);
        } catch (Exception e) { throw new RuntimeException(e); }

        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(teacher));
        when(courseRepository.findByTeacherId(1L)).thenReturn(List.of(mock(Course.class), mock(Course.class)));

        SessionSnapshot s1 = mock(SessionSnapshot.class);
        when(s1.getGreenCount()).thenReturn(10L);
        when(s1.getYellowCount()).thenReturn(5L);
        when(s1.getRedCount()).thenReturn(3L);
        SessionSnapshot s2 = mock(SessionSnapshot.class);
        when(s2.getGreenCount()).thenReturn(8L);
        when(s2.getYellowCount()).thenReturn(4L);
        when(s2.getRedCount()).thenReturn(2L);
        when(snapshotRepository.findBySessionCourseTeacherId(1L)).thenReturn(List.of(s1, s2));

        LifetimeStatsResponse result = statsService.getLifetimeStats("t1");

        assertEquals(2, result.courseCount());
        assertEquals(2, result.sessionCount());
        assertEquals(18, result.totalGreen());
        assertEquals(9, result.totalYellow());
        assertEquals(5, result.totalRed());
        assertEquals(32, result.totalStudentResponses());
    }
}
