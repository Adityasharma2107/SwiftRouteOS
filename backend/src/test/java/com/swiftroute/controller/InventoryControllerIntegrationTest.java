package com.swiftroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.entity.Skill;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.domain.repository.ServiceRequestRepository;
import com.swiftroute.domain.repository.SkillRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SkillRepository skillRepository;

    private String dispatcherToken;
    private String techToken;

    @BeforeEach
    void setupTokens() throws Exception {
        // Obtain Dispatcher Token
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

        // Obtain Technician Token
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

    private Long createTestJob() {
        Skill skill = skillRepository.findAll().stream().findFirst().orElseThrow();
        ServiceRequest sr = new ServiceRequest(
                "Acme Towers",
                "212-555-0100",
                "100 Park Ave, New York, NY",
                40.7516,
                -73.9776,
                "Chiller Leak",
                "Severe coolant leak on main compressor",
                Priority.HIGH,
                skill,
                90,
                "PENDING"
        );
        ServiceRequest savedSr = serviceRequestRepository.save(sr);
        Job job = new Job(
                savedSr,
                Priority.HIGH,
                JobStatus.PENDING,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(14400),
                null
        );
        return jobRepository.save(job).getId();
    }

    @Test
    @DisplayName("GET /api/inventory/items returns catalog items with stock metrics")
    void testGetAllItemsAuthorized() throws Exception {
        mockMvc.perform(get("/api/inventory/items")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].partNumber").isString())
                .andExpect(jsonPath("$.data[0].quantityOnHand").isNumber())
                .andExpect(jsonPath("$.data[0].availableQuantity").isNumber());
    }

    @Test
    @DisplayName("GET /api/inventory/items without token returns 401 Unauthorized")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/inventory/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/inventory/items/{id} returns single item or 404 if missing")
    void testGetItemById() throws Exception {
        // Fetch first item ID
        MvcResult listRes = mockMvc.perform(get("/api/inventory/items")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listNode = objectMapper.readTree(listRes.getResponse().getContentAsString());
        long firstItemId = listNode.path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/inventory/items/" + firstItemId)
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstItemId));

        // Non-existent item returns 404
        mockMvc.perform(get("/api/inventory/items/999999")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/inventory/items/low-stock returns items below reorder threshold")
    void testGetLowStockItems() throws Exception {
        mockMvc.perform(get("/api/inventory/items/low-stock")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/inventory/reserve locks parts with pessimistic lock and returns 201 Created")
    void testReserveAndReleaseParts() throws Exception {
        Long testJobId = createTestJob();

        // Get first inventory item ID
        MvcResult listRes = mockMvc.perform(get("/api/inventory/items")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listNode = objectMapper.readTree(listRes.getResponse().getContentAsString());
        long itemId = 1;
        for (JsonNode item : listNode.path("data")) {
            if (!"SCARCE-SENSOR-CHILLER".equals(item.path("partNumber").asText()) && item.path("availableQuantity").asInt() > 0) {
                itemId = item.path("id").asLong();
                break;
            }
        }

        String reservePayload = String.format("""
            {
                "jobId": %d,
                "items": [
                    { "inventoryItemId": %d, "quantity": 1 }
                ],
                "notes": "Emergency repair kit"
            }
        """, testJobId, itemId);

        mockMvc.perform(post("/api/inventory/reserve")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("RESERVED"));

        // Verify active reservation can be fetched
        mockMvc.perform(get("/api/inventory/jobs/" + testJobId + "/reservations")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("RESERVED"));

        // Release reservation
        mockMvc.perform(post("/api/inventory/jobs/" + testJobId + "/release")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .param("reason", "Customer cancelled maintenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("RELEASED"));
    }

    @Test
    @DisplayName("POST /api/inventory/reserve returns 403 Forbidden for TECHNICIAN role")
    void testTechnicianCannotReserve() throws Exception {
        Long testJobId = createTestJob();

        String reservePayload = String.format("""
            {
                "jobId": %d,
                "items": [
                    { "inventoryItemId": 1, "quantity": 1 }
                ]
            }
        """, testJobId);

        mockMvc.perform(post("/api/inventory/reserve")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/inventory/chaos-test runs multi-threaded race simulation for DISPATCHER")
    void testChaosTestEndpoint() throws Exception {
        mockMvc.perform(post("/api/inventory/chaos-test")
                        .header("Authorization", "Bearer " + dispatcherToken)
                        .param("threads", "5")
                        .param("resetAfterTest", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.concurrencyLevel").value(5))
                .andExpect(jsonPath("$.data.zeroOversellGuaranteed").value(true));

        // Technician cannot trigger chaos test
        mockMvc.perform(post("/api/inventory/chaos-test")
                        .header("Authorization", "Bearer " + techToken)
                        .param("threads", "5"))
                .andExpect(status().isForbidden());
    }
}
