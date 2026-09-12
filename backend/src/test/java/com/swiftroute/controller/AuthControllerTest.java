package com.swiftroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Admin user logs in successfully and receives JWT tokens")
    void testAdminLoginSuccess() throws Exception {
        String loginPayload = """
            {
                "username": "admin",
                "password": "password123"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Dispatcher user logs in successfully with correct role")
    void testDispatcherLoginSuccess() throws Exception {
        String loginPayload = """
            {
                "username": "dispatcher",
                "password": "password123"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("dispatcher"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_DISPATCHER"));
    }

    @Test
    @DisplayName("Invalid credentials return 401 UNAUTHORIZED")
    void testInvalidCredentials() throws Exception {
        String badPayload = """
            {
                "username": "admin",
                "password": "wrongpassword"
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Blank credentials fail Jakarta validation with 400 BAD REQUEST")
    void testValidationFailure() throws Exception {
        String emptyPayload = """
            {
                "username": "",
                "password": ""
            }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Protected /api/auth/me returns profile when valid Bearer token is provided")
    void testGetMeWithToken() throws Exception {
        // 1. Log in as technician Dave
        String loginPayload = """
            {
                "username": "tech_dave",
                "password": "password123"
            }
        """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = root.path("data").path("accessToken").asText();
        assertNotNull(token);

        // 2. Call /api/auth/me with Bearer token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("tech_dave"))
                .andExpect(jsonPath("$.data.role").value("ROLE_TECHNICIAN"));
    }

    @Test
    @DisplayName("Protected /api/auth/me without token returns 401 UNAUTHORIZED")
    void testGetMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Refresh token generates new valid access token")
    void testRefreshToken() throws Exception {
        // 1. Log in
        String loginPayload = """
            {
                "username": "dispatcher",
                "password": "password123"
            }
        """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = root.path("data").path("refreshToken").asText();

        // 2. Refresh
        String refreshPayload = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.username").value("dispatcher"));
    }
}
