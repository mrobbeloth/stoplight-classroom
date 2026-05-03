package com.stoplight.classroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplight.classroom.dto.*;
import com.stoplight.classroom.model.ActivityMode;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.model.StoplightValue;
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

@SpringBootTest
@AutoConfigureMockMvc
class SessionFlowIntegrationTest {

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

    @Test
    void fullSessionFlow() throws Exception {
        String teacherToken = createTeacher("flow_teacher");

        // 1. Create course
        String courseBody = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest("CS201", "Spring 2026"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long courseId = objectMapper.readTree(courseBody).get("id").asLong();

        // 2. Start session
        String sessionBody = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joinCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionBody).get("id").asLong();
        String joinCode = objectMapper.readTree(sessionBody).get("joinCode").asText();

        // 3. Student joins
        String joinBody = mockMvc.perform(post("/api/sessions/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinSessionRequest(joinCode, "Alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String studentToken = objectMapper.readTree(joinBody).get("participantToken").asText();

        // 4. Student submits GREEN
        mockMvc.perform(post("/api/stoplight/" + sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StoplightRequest(StoplightValue.GREEN))))
                .andExpect(status().isOk());

        // 5. Check aggregate
        mockMvc.perform(get("/api/sessions/" + sessionId + "/aggregate")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.green").value(1))
                .andExpect(jsonPath("$.total").value(1));

        // 6. Student updates to RED
        mockMvc.perform(post("/api/stoplight/" + sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StoplightRequest(StoplightValue.RED))))
                .andExpect(status().isOk());

        // 7. Aggregate reflects update
        mockMvc.perform(get("/api/sessions/" + sessionId + "/aggregate")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.green").value(0))
                .andExpect(jsonPath("$.red").value(1));

        // 8. Set activity mode
        mockMvc.perform(put("/api/sessions/" + sessionId + "/activity-mode")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityModeRequest(ActivityMode.GROUP))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityMode").value("GROUP"));

        // 9. End session
        mockMvc.perform(put("/api/sessions/" + sessionId + "/end")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));

        // 10. Can't submit after end
        mockMvc.perform(post("/api/stoplight/" + sessionId)
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StoplightRequest(StoplightValue.YELLOW))))
                .andExpect(status().isBadRequest());

        // 11. Session stats available after end
        mockMvc.perform(get("/api/stats/session/" + sessionId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redCount").value(1))
                .andExpect(jsonPath("$.studentCount").value(1));

        // 12. Course stats
        mockMvc.perform(get("/api/stats/course/" + courseId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(1))
                .andExpect(jsonPath("$.totalRed").value(1));

        // 13. Lifetime stats
        mockMvc.perform(get("/api/stats/lifetime")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(1));
    }

    @Test
    void cannotStartTwoActiveSessions() throws Exception {
        String teacherToken = createTeacher("dual_teacher");

        // Create course
        String courseBody = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCourseRequest("CS301", "Fall 2026"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long courseId = objectMapper.readTree(courseBody).get("id").asLong();

        // Start first session
        String sessionBody = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(sessionBody).get("id").asLong();

        // Second session fails
        mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isBadRequest());

        // End first, then start another
        mockMvc.perform(put("/api/sessions/" + sessionId + "/end")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartSessionRequest(courseId))))
                .andExpect(status().isCreated());
    }

    @Test
    void joinWithInvalidCode_returns400() throws Exception {
        mockMvc.perform(post("/api/sessions/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinSessionRequest("BADCODE", "Bob"))))
                .andExpect(status().isBadRequest());
    }
}
