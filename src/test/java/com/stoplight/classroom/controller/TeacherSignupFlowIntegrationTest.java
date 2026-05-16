package com.stoplight.classroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stoplight.classroom.dto.TeacherSignupRequest;
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

/** End-to-end integration test for the public teacher signup + admin approval flow. */
@SpringBootTest
@AutoConfigureMockMvc
class TeacherSignupFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Admin is seeded by AdminSeedConfig
        adminToken = jwtUtil.generateAccessToken("admin", "ADMIN");
    }

    @Test
    void signup_thenApprove_thenLogin_succeeds() throws Exception {
        var req = new TeacherSignupRequest("flow_prof", "flow_prof@example.edu", "password123");

        // 1. Public signup → 201, status PENDING
        String body = mockMvc.perform(post("/api/auth/teacher/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("flow_prof"))
                .andExpect(jsonPath("$.email").value("flow_prof@example.edu"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        // 2. Login while pending → 400
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"flow_prof\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());

        // 3. Admin sees pending signup
        mockMvc.perform(get("/api/admin/teacher-signups")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("PENDING"));

        // 4. Admin approves
        mockMvc.perform(post("/api/admin/teacher-signups/" + id + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 5. Login now succeeds
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"flow_prof\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void signup_nonEduEmail_returns400() throws Exception {
        var req = new TeacherSignupRequest("bad_prof", "bad@gmail.com", "password123");
        mockMvc.perform(post("/api/auth/teacher/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_thenReject_thenLogin_fails() throws Exception {
        var req = new TeacherSignupRequest("rej_prof", "rej_prof@example.edu", "password123");

        String body = mockMvc.perform(post("/api/auth/teacher/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(post("/api/admin/teacher-signups/" + id + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rej_prof\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminSignupEndpoints_withoutAdminToken_areForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/teacher-signups"))
                .andExpect(status().isUnauthorized());
    }
}
