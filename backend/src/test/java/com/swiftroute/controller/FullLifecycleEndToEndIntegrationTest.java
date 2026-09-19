package com.swiftroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.repository.AuditEventRepository;
import com.swiftroute.domain.repository.JobRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FullLifecycleEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private com.swiftroute.domain.repository.AssignmentRepository assignmentRepository;

    private String dispatcherToken;
    private String techToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        assignmentRepository.deleteAll();

        // Dispatcher login
        String dispLogin = """
            { "username": "dispatcher", "password": "password123" }
        """;
        MvcResult dispRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dispLogin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dispNode = objectMapper.readTree(dispRes.getResponse().getContentAsString());
        this.dispatcherToken = dispNode.path("data").path("accessToken").asText();

        // Technician login
        String techLogin = """
            { "username": "tech_dave", "password": "password123" }
        """;
        MvcResult techRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(techLogin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode techNode = objectMapper.readTree(techRes.getResponse().getContentAsString());
        this.techToken = techNode.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete E2E Operational Lifecycle: Intake -> Score -> Dispatch -> Reserve -> En Route -> In Progress -> Completed -> Audit")
    void testCompleteOperationalLifecycle() throws Exception {
        // Step 1: Customer Service Request Intake
        String serviceRequestPayload = """
            {
                "customerName": "Empire State Building Corp",
                "customerPhone": "212-555-7000",
                "serviceAddress": "350 5th Ave, New York, NY 10118",
                "latitude": 40.7484,
                "longitude": -73.9857,
                "title": "Chiller Compressor Trip",
                "description": "Primary cooling tower pump overheating, requires immediate diagnostic",
                "priority": "HIGH",
                "requiredSkillId": 1,
                "estimatedDurationMinutes": 90
            }
        """;

        MvcResult srResult = mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceRequestPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").isNumber())
                .andReturn();

        JsonNode srNode = objectMapper.readTree(srResult.getResponse().getContentAsString());
        long jobId = srNode.path("data").path("jobId").asLong();
        assertTrue(jobId > 0, "Generated Job ID must be positive");

        // Step 2: Verify Initial Job State (PENDING with SLA response/resolution deadlines)
        MvcResult jobDetailsResult = mockMvc.perform(get("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.slaResponseDeadline").isNotEmpty())
                .andExpect(jsonPath("$.data.slaResolutionDeadline").isNotEmpty())
                .andReturn();

        // Step 3: Dispatcher Queries Multi-Factor Candidate Ranking
        MvcResult recResult = mockMvc.perform(get("/api/dispatch/recommendations/" + jobId)
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidates", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.recommendedCandidate.technicianId").isNumber())
                .andExpect(jsonPath("$.data.recommendedCandidate.scoreBreakdown.totalScore").isNumber())
                .andReturn();

        JsonNode recNode = objectMapper.readTree(recResult.getResponse().getContentAsString());
        long assignedTechId = recNode.path("data").path("recommendedCandidate").path("technicianId").asLong();

        // Step 4: Dispatcher Confirms Assignment
        Instant startTime = Instant.now().plus(15, ChronoUnit.MINUTES);
        Instant endTime = startTime.plus(90, ChronoUnit.MINUTES);

        String confirmPayload = String.format("""
            {
                "jobId": %d,
                "selectedTechnicianId": %d,
                "scheduledStartTime": "%s",
                "scheduledEndTime": "%s",
                "overrideReason": "Assigned to top scoring candidate by dispatch protocol"
            }
        """, jobId, assignedTechId, startTime.toString(), endTime.toString());

        mockMvc.perform(post("/api/dispatch/confirm")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.technicianId").value(assignedTechId));

        // Step 5: Reserve Spare Parts with Pessimistic Locking
        // Fetch first inventory item ID
        MvcResult itemsResult = mockMvc.perform(get("/api/inventory/items")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode itemsNode = objectMapper.readTree(itemsResult.getResponse().getContentAsString());
        long inventoryItemId = 1;
        for (JsonNode item : itemsNode.path("data")) {
            if (!"SCARCE-SENSOR-CHILLER".equals(item.path("partNumber").asText()) && item.path("availableQuantity").asInt() > 0) {
                inventoryItemId = item.path("id").asLong();
                break;
            }
        }

        String reservePayload = String.format("""
            {
                "jobId": %d,
                "items": [
                    { "inventoryItemId": %d, "quantity": 1 }
                ],
                "notes": "Required sensor component for HVAC repair"
            }
        """, jobId, inventoryItemId);

        mockMvc.perform(post("/api/inventory/reserve")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("RESERVED"));

        // Step 6: Technician Dave marks Job EN_ROUTE
        String enRoutePayload = """
            { "targetStatus": "EN_ROUTE" }
        """;
        mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enRoutePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EN_ROUTE"));

        // Step 7: Technician Dave arrives on-site and marks IN_PROGRESS
        String inProgressPayload = """
            { "targetStatus": "IN_PROGRESS" }
        """;
        MvcResult inProgResult = mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inProgressPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.actualResponseAt").isNotEmpty())
                .andReturn();

        JsonNode inProgNode = objectMapper.readTree(inProgResult.getResponse().getContentAsString());
        assertNotNull(inProgNode.path("data").path("actualResponseAt").asText());

        // Step 8: Technician Dave completes repair and marks COMPLETED
        String completedPayload = """
            { "targetStatus": "COMPLETED" }
        """;
        MvcResult completedResult = mockMvc.perform(patch("/api/jobs/" + jobId + "/transition")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.actualResolutionAt").isNotEmpty())
                .andReturn();

        JsonNode completedNode = objectMapper.readTree(completedResult.getResponse().getContentAsString());
        assertNotNull(completedNode.path("data").path("actualResolutionAt").asText());

        // Step 9: Verify Parts Transition to CONSUMED upon Job Completion
        mockMvc.perform(get("/api/inventory/jobs/" + jobId + "/reservations")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CONSUMED"));

        // Step 10: Verify Complete Immutable Audit Log
        List<AuditEvent> audits = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("JOB", jobId);
        assertFalse(audits.isEmpty(), "Audit log should contain recorded lifecycle events");

        boolean hasDispatchedOrCreated = audits.stream().anyMatch(a -> a.getAction() == AuditAction.DISPATCHED || a.getAction() == AuditAction.CREATED);
        boolean hasStatusChanged = audits.stream().anyMatch(a -> a.getAction() == AuditAction.STATUS_CHANGED);
        assertTrue(hasDispatchedOrCreated, "Audit log must track job assignment or creation");
        assertTrue(hasStatusChanged, "Audit log must track state transitions");
    }
}
