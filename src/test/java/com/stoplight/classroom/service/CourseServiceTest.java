package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.CourseResponse;
import com.stoplight.classroom.dto.CreateCourseRequest;
import com.stoplight.classroom.exception.ResourceNotFoundException;
import com.stoplight.classroom.model.Course;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.User;
import com.stoplight.classroom.repository.CourseRepository;
import com.stoplight.classroom.repository.UserRepository;
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
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;

    private CourseService courseService;
    private User teacher;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, userRepository);
        teacher = new User("teacher1", "hash", Role.TEACHER);
        // Use reflection to set ID since there's no setter
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(teacher, 1L);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void create_success() {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseResponse response = courseService.create("teacher1", new CreateCourseRequest("CS101", "Fall 2026"));

        assertEquals("CS101", response.name());
        assertEquals("Fall 2026", response.term());
    }

    @Test
    void listForTeacher_returnsOwnCourses() {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacher));
        Course c = new Course("CS101", "Fall", teacher);
        when(courseRepository.findByTeacherId(1L)).thenReturn(List.of(c));

        List<CourseResponse> result = courseService.listForTeacher("teacher1");

        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).name());
    }

    @Test
    void findOwned_wrongTeacher_throws() {
        User other = new User("other", "hash", Role.TEACHER);
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(other, 2L);
        } catch (Exception e) { throw new RuntimeException(e); }

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        Course c = new Course("CS101", "Fall", teacher);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(ResourceNotFoundException.class, () -> courseService.findOwned("other", 1L));
    }
}
