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

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceRequestWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String dispatcherToken;
    private String techToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        // Obtain dispatcher token
        String dispatcherLogin = """
            { "username": "dispatcher", "password": "password123" }
        """;
        MvcResult dispResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispatcherLogin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dispNode = objectMapper.readTree(dispResult.getResponse().getContentAsString());
        this.dispatcherToken = dispNode.path("data").path("accessToken").asText();

        // Obtain technician token
        String techLogin = """
            { "username": "tech_dave", "password": "password123" }
        """;
        MvcResult techResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(techLogin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode techNode = objectMapper.readTree(techResult.getResponse().getContentAsString());
        this.techToken = techNode.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Dispatcher creates CRITICAL request: SLA response (60m) and resolution (240m) deadlines are computed")
    void testCreateCriticalServiceRequest() throws Exception {
        String payload = """
            {
                "customerName": "Mount Sinai Hospital",
                "customerPhone": "212-555-0199",
                "serviceAddress": "1 Gustave L. Levy Pl, New York, NY 10029",
                "latitude": 40.7900,
                "longitude": -73.9530,
                "title": "Emergency Chiller Offline - ICU Wing",
                "description": "Critical cooling failure in medical ICU ward",
                "priority": "CRITICAL",
                "requiredSkillId": 1,
                "estimatedDurationMinutes": 90,
                "requiredParts": [
                    { "inventoryItemId": 1, "quantity": 1 }
                ]
            }
        """;

        Instant before = Instant.now();

        MvcResult result = mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerName").value("Mount Sinai Hospital"))
                .andExpect(jsonPath("$.data.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.data.jobId").isNumber())
                .andExpect(jsonPath("$.data.job.status").value("PENDING"))
                .andExpect(jsonPath("$.data.job.slaStatus").value("HEALTHY"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode job = root.path("data").path("job");

        Instant responseDeadline = Instant.parse(job.path("slaResponseDeadline").asText());
        Instant resolutionDeadline = Instant.parse(job.path("slaResolutionDeadline").asText());

        // Policy for CRITICAL is 60m response, 240m resolution
        long responseMinutes = Duration.between(before, responseDeadline).toMinutes();
        long resolutionMinutes = Duration.between(before, resolutionDeadline).toMinutes();

        assertTrue(responseMinutes >= 58 && responseMinutes <= 62,
                "Expected response deadline ~60 min from now, but got: " + responseMinutes);
        assertTrue(resolutionMinutes >= 238 && resolutionMinutes <= 242,
                "Expected resolution deadline ~240 min from now, but got: " + resolutionMinutes);

        // Verify required parts attached
        assertEquals(1, job.path("requiredParts").size());
        assertEquals("CAP-45UF-440V", job.path("requiredParts").get(0).path("partNumber").asText());
    }

    @Test
    @DisplayName("Request with non-existent skill returns 404 RESOURCE_NOT_FOUND")
    void testNonExistentSkillFails() throws Exception {
        String badPayload = """
            {
                "customerName": "Test Customer",
                "customerPhone": "212-555-0100",
                "serviceAddress": "100 Broadway, New York, NY",
                "latitude": 40.7081,
                "longitude": -74.0113,
                "title": "Fix Wiring",
                "priority": "HIGH",
                "requiredSkillId": 99999,
                "estimatedDurationMinutes": 60
            }
        """;

        mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Technician role is forbidden (403) from creating service requests")
    void testTechnicianForbiddenFromIntake() throws Exception {
        String payload = """
            {
                "customerName": "Unauthorized Customer",
                "customerPhone": "212-555-0123",
                "serviceAddress": "100 Main St, NY",
                "latitude": 40.7128,
                "longitude": -74.0060,
                "title": "Direct Request",
                "priority": "LOW",
                "requiredSkillId": 1,
                "estimatedDurationMinutes": 60
            }
        """;

        mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Query jobs with status and priority filters")
    void testListJobsWithFilters() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .param("status", "PENDING")
                        .param("priority", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
