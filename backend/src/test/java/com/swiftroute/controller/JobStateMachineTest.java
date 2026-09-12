package com.swiftroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobStateMachineTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String dispatcherToken;

    @BeforeEach
    void obtainDispatcherToken() throws Exception {
        String loginPayload = """
            { "username": "dispatcher", "password": "password123" }
        """;
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        this.dispatcherToken = node.path("data").path("accessToken").asText();
    }

    private Long createTestJob() throws Exception {
        String payload = """
            {
                "customerName": "Grand Central Terminal",
                "customerPhone": "212-555-8900",
                "serviceAddress": "89 E 42nd St, New York, NY 10017",
                "latitude": 40.7527,
                "longitude": -73.9772,
                "title": "HVAC Air Flow Failure",
                "description": "Main concourse blower stopped working",
                "priority": "HIGH",
                "requiredSkillId": 1,
                "estimatedDurationMinutes": 60
            }
        """;

        MvcResult result = mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("jobId").asLong();
    }

    @Test
    @DisplayName("Valid state sequence: PENDING -> ASSIGNED -> EN_ROUTE -> IN_PROGRESS -> COMPLETED with timestamps")
    void testValidStateProgression() throws Exception {
        Long jobId = createTestJob();

        // 1. PENDING -> ASSIGNED
        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"ASSIGNED\", \"notes\": \"Assigned to tech\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));

        // 2. ASSIGNED -> EN_ROUTE
        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"EN_ROUTE\", \"notes\": \"Technician driving to site\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EN_ROUTE"));

        // 3. EN_ROUTE -> IN_PROGRESS: actualResponseAt must be recorded
        MvcResult inProgResult = mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"IN_PROGRESS\", \"notes\": \"Arrived on-site and began work\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn();

        JsonNode inProgNode = objectMapper.readTree(inProgResult.getResponse().getContentAsString());
        String actualResponseAt = inProgNode.path("data").path("actualResponseAt").asText();
        assertNotNull(actualResponseAt, "actualResponseAt must be set when moving to IN_PROGRESS");
        assertFalse(actualResponseAt.isBlank());

        // 4. IN_PROGRESS -> COMPLETED: actualResolutionAt must be recorded
        MvcResult compResult = mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"COMPLETED\", \"notes\": \"Repairs verified and tested\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn();

        JsonNode compNode = objectMapper.readTree(compResult.getResponse().getContentAsString());
        String actualResolutionAt = compNode.path("data").path("actualResolutionAt").asText();
        assertNotNull(actualResolutionAt, "actualResolutionAt must be set when moving to COMPLETED");
        assertFalse(actualResolutionAt.isBlank());
    }

    @Test
    @DisplayName("Illegal transition directly from PENDING to COMPLETED is rejected with 422 UNPROCESSABLE_ENTITY")
    void testIllegalTransitionFails() throws Exception {
        Long jobId = createTestJob();

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"COMPLETED\", \"notes\": \"Trying to jump straight to done\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.details.fromStatus").value("PENDING"))
                .andExpect(jsonPath("$.error.details.toStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("Terminal COMPLETED state rejects any further transition attempts")
    void testTerminalStateRejection() throws Exception {
        Long jobId = createTestJob();

        // Advance to COMPLETED through valid states
        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                .header("Authorization", "Bearer " + dispatcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\": \"ASSIGNED\"}"));

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                .header("Authorization", "Bearer " + dispatcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\": \"EN_ROUTE\"}"));

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                .header("Authorization", "Bearer " + dispatcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\": \"IN_PROGRESS\"}"));

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                .header("Authorization", "Bearer " + dispatcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\": \"COMPLETED\"}"));

        // Attempt transition from COMPLETED back to IN_PROGRESS
        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"IN_PROGRESS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.error.details.fromStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("Cancellation from ASSIGNED state succeeds")
    void testCancellation() throws Exception {
        Long jobId = createTestJob();

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                .header("Authorization", "Bearer " + dispatcherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetStatus\": \"ASSIGNED\"}"));

        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\": \"CANCELLED\", \"notes\": \"Customer called to cancel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
