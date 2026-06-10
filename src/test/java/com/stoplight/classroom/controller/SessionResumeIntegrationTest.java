package com.stoplight.classroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplight.classroom.dto.CreateCourseRequest;
import com.stoplight.classroom.dto.CreateUserRequest;
import com.stoplight.classroom.dto.StartSessionRequest;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the session "resume" affordances added in issue #1:
 * - GET /api/sessions/active
 * - GET /api/courses/{id}/sessions
 * - POST /api/sessions returning 409 when an active session already exists
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionResumeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateAccessToken("admin", "ADMIN");
    }

    private String createTeacher(String username) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateUserRequest(username, "password123", Role.TEACHER))))
                .andExpect(status().isCreated());
        return jwtUtil.generateAccessToken(username, "TEACHER");
    }

    private Long createCourse(String teacherToken, String name) throws Exception {
        String body = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest(name, "Fall 2026"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long startSession(String teacherToken, Long courseId) throws Exception {
        String body = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void getActiveSession_whenNoActive_returns204() throws Exception {
        String teacherToken = createTeacher("resume_t1");

        mockMvc.perform(get("/api/sessions/active")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getActiveSession_whenActive_returnsSession() throws Exception {
        String teacherToken = createTeacher("resume_t2");
        Long courseId = createCourse(teacherToken, "CS101");
        Long sessionId = startSession(teacherToken, courseId);

        mockMvc.perform(get("/api/sessions/active")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getActiveSession_afterEnding_returns204() throws Exception {
        String teacherToken = createTeacher("resume_t3");
        Long courseId = createCourse(teacherToken, "CS102");
        Long sessionId = startSession(teacherToken, courseId);

        mockMvc.perform(put("/api/sessions/" + sessionId + "/end")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sessions/active")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getActiveSession_isPerTeacher() throws Exception {
        String t1 = createTeacher("resume_owner");
        String t2 = createTeacher("resume_other");
        Long courseId = createCourse(t1, "CS201");
        startSession(t1, courseId);

        mockMvc.perform(get("/api/sessions/active")
                        .header("Authorization", "Bearer " + t2))
                .andExpect(status().isNoContent());
    }

    @Test
    void startSession_whenActiveExists_returns409WithExistingSession() throws Exception {
        String teacherToken = createTeacher("resume_t4");
        Long courseId = createCourse(teacherToken, "CS301");
        Long sessionId = startSession(teacherToken, courseId);

        mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.session.id").value(sessionId))
                .andExpect(jsonPath("$.session.courseId").value(courseId))
                .andExpect(jsonPath("$.session.status").value("ACTIVE"))
                .andExpect(jsonPath("$.session.joinCode").isNotEmpty());
    }

    @Test
    void listCourseSessions_returnsHistoryNewestFirst() throws Exception {
        String teacherToken = createTeacher("resume_t5");
        Long courseId = createCourse(teacherToken, "CS401");

        Long s1 = startSession(teacherToken, courseId);
        mockMvc.perform(put("/api/sessions/" + s1 + "/end")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        Long s2 = startSession(teacherToken, courseId);
        mockMvc.perform(put("/api/sessions/" + s2 + "/end")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/" + courseId + "/sessions")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(s2))
                .andExpect(jsonPath("$[1].id").value(s1));
    }

    @Test
    void listCourseSessions_rejectsNonOwner() throws Exception {
        String owner = createTeacher("resume_owner2");
        String other = createTeacher("resume_other2");
        Long courseId = createCourse(owner, "CS501");
        startSession(owner, courseId);

        mockMvc.perform(get("/api/courses/" + courseId + "/sessions")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }
}
