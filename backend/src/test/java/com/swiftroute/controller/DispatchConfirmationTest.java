package com.swiftroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchConfirmationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private com.swiftroute.domain.repository.AssignmentRepository assignmentRepository;

    private String dispatcherToken;
    private String techToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        assignmentRepository.deleteAll();

        String dispLogin = """
            { "username": "dispatcher", "password": "password123" }
        """;
        MvcResult dispResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispLogin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dispNode = objectMapper.readTree(dispResult.getResponse().getContentAsString());
        this.dispatcherToken = dispNode.path("data").path("accessToken").asText();

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

    private Long createTestJob(String title, Long skillId) throws Exception {
        String payload = String.format("""
            {
                "customerName": "Test Customer Dispatch",
                "customerPhone": "212-555-0999",
                "serviceAddress": "350 5th Ave, New York, NY 10118",
                "latitude": 40.7484,
                "longitude": -73.9857,
                "title": "%s",
                "description": "Integration test job for dispatch confirmation",
                "priority": "HIGH",
                "requiredSkillId": %d,
                "estimatedDurationMinutes": 60
            }
        """, title, skillId);

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
    @DisplayName("Dispatcher confirms assignment for top recommended candidate without override")
    void testConfirmDispatchTopCandidate() throws Exception {
        // Skill 2 = HVAC_LVL2
        Long jobId = createTestJob("Emergency Chiller Repair", 2L);

        // Fetch recommendation
        MvcResult recResult = mockMvc.perform(get("/api/dispatch/recommendations/" + jobId)
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode recData = objectMapper.readTree(recResult.getResponse().getContentAsString()).path("data");
        Long topTechId = recData.path("recommendedCandidate").path("technicianId").asLong();
        String topTechName = recData.path("recommendedCandidate").path("technicianName").asText();

        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        String confirmPayload = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": %d,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s",
                "notes": "Standard dispatch assignment"
            }
        """, jobId, topTechId, start, end);

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId))
                .andExpect(jsonPath("$.data.technicianId").value(topTechId))
                .andExpect(jsonPath("$.data.technicianName").value(topTechName))
                .andExpect(jsonPath("$.data.jobStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.manualOverride").value(false))
                .andExpect(jsonPath("$.data.assignedBy").value("dispatcher"));

        // Verify DISPATCHED audit event was logged
        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("JOB", jobId);
        boolean hasDispatchedEvent = events.stream().anyMatch(e -> e.getAction() == AuditAction.DISPATCHED);
        assertTrue(hasDispatchedEvent, "Expected AuditAction.DISPATCHED event to be logged for job");
    }

    @Test
    @DisplayName("Technician scheduling conflict throws 409 CONFLICT on overlapping time window")
    void testSchedulingConflictOverlapInvariant() throws Exception {
        Long jobA = createTestJob("Job A Schedule", 2L);
        Long jobB = createTestJob("Job B Schedule Conflict", 2L);

        Instant start = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // Assign Job A to Technician 1 (Dave)
        String payloadA = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": 1,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s"
            }
        """, jobA, start, end);

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadA))
                .andExpect(status().isCreated());

        // Attempt to assign Job B to Technician 1 with overlapping time window (start + 1 hour)
        Instant overlapStart = start.plus(1, ChronoUnit.HOURS);
        Instant overlapEnd = overlapStart.plus(2, ChronoUnit.HOURS);

        String payloadB = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": 1,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s"
            }
        """, jobB, overlapStart, overlapEnd);

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadB))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("TECHNICIAN_CONFLICT"))
                .andExpect(jsonPath("$.error.details.technicianId").value(1));
    }

    @Test
    @DisplayName("Manual override of #1 recommendation requires mandatory overrideReason")
    void testManualOverrideValidationAndAuditing() throws Exception {
        Long jobId = createTestJob("Job With Candidate Selection", 2L);

        // Dynamically fetch recommendations to identify #1 vs #2 candidate
        MvcResult recResult = mockMvc.perform(get("/api/dispatch/recommendations/" + jobId)
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode recData = objectMapper.readTree(recResult.getResponse().getContentAsString()).path("data");
        JsonNode candidates = recData.path("candidates");
        assertTrue(candidates.size() >= 2, "Expected at least 2 eligible candidates for HVAC_LVL2");

        JsonNode nonTopCandidate = candidates.get(1);
        Long nonTopTechId = nonTopCandidate.path("technicianId").asLong();
        String nonTopTechName = nonTopCandidate.path("technicianName").asText();

        Instant start = Instant.now().plus(6, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // 1. Attempt override WITHOUT overrideReason -> Expect 400 Bad Request
        String noReasonPayload = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": %d,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s"
            }
        """, jobId, nonTopTechId, start, end);

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noReasonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("overrideReason is mandatory")));

        // 2. Now provide valid overrideReason -> Expect 201 Created
        String withReasonPayload = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": %d,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s",
                "overrideReason": "Customer requested %s for specialized facility protocol"
            }
        """, jobId, nonTopTechId, start, end, nonTopTechName);

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withReasonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.technicianId").value(nonTopTechId))
                .andExpect(jsonPath("$.data.manualOverride").value(true))
                .andExpect(jsonPath("$.data.overrideReason").value("Customer requested " + nonTopTechName + " for specialized facility protocol"));

        // Verify MANUAL_OVERRIDE audit event is logged
        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("JOB", jobId);
        boolean hasOverrideEvent = events.stream().anyMatch(e -> e.getAction() == AuditAction.MANUAL_OVERRIDE);
        assertTrue(hasOverrideEvent, "Expected AuditAction.MANUAL_OVERRIDE event to be recorded in audit log");
    }

    @Test
    @DisplayName("Assigning an already ASSIGNED job throws 422 INVALID_STATE_TRANSITION")
    void testAssigningAlreadyAssignedJobThrows422() throws Exception {
        Long jobId = createTestJob("Single Assignment Only Job", 2L);

        Instant start = Instant.now().plus(9, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        String payload = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": 1,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s"
            }
        """, jobId, start, end);

        // First assignment succeeds
        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Second assignment on same job fails
        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    @DisplayName("Technician role is forbidden from confirming dispatch assignments")
    void testTechnicianForbiddenFromConfirmDispatch() throws Exception {
        String payload = """
            {
                "jobId": 1,
                "selectedTechnicianId": 1,
                "scheduledStartTime": "2026-10-01T10:00:00Z",
                "scheduledEndTime": "2026-10-01T12:00:00Z"
            }
        """;

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("FORBIDDEN"));
    }
}
