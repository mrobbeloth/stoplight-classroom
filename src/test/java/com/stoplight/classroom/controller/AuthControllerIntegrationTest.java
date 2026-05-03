package com.stoplight.classroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplight.classroom.dto.CreateUserRequest;
import com.stoplight.classroom.model.Role;
import com.stoplight.classroom.repository.UserRepository;
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
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository userRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Admin is seeded by AdminSeedConfig
        adminToken = jwtUtil.generateAccessToken("admin", "ADMIN");
    }

    @Test
    void login_withSeededAdmin_returnsTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"testpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_invalidCredentials_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminEndpoint_withTeacherToken_returns403() throws Exception {
        // Create a teacher first
        var req = new CreateUserRequest("teacher_test", "password123", Role.TEACHER);
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        String teacherToken = jwtUtil.generateAccessToken("teacher_test", "TEACHER");
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndDeleteUser_fullCycle() throws Exception {
        var req = new CreateUserRequest("lifecycle_user", "password123", Role.TEACHER);

        // Create
        String body = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("lifecycle_user"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        // Get
        mockMvc.perform(get("/api/admin/users/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lifecycle_user"));

        // Delete
        mockMvc.perform(delete("/api/admin/users/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify gone
        mockMvc.perform(get("/api/admin/users/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
