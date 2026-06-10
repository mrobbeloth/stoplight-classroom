package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.CourseResponse;
import com.stoplight.classroom.dto.CreateCourseRequest;
import com.stoplight.classroom.dto.SessionResponse;
import com.stoplight.classroom.service.CourseService;
import com.stoplight.classroom.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final SessionService sessionService;

    public CourseController(CourseService courseService, SessionService sessionService) {
        this.courseService = courseService;
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(Authentication auth,
                                                  @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.create(auth.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> list(Authentication auth) {
        return ResponseEntity.ok(courseService.listForTeacher(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> get(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(courseService.get(auth.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> update(Authentication auth, @PathVariable Long id,
                                                  @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.update(auth.getName(), id, request));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archive(Authentication auth, @PathVariable Long id) {
        courseService.archive(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(sessionService.listSessionsForCourse(auth.getName(), id));
    }
}
