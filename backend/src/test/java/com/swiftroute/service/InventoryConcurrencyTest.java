package com.swiftroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftroute.domain.entity.InventoryItem;
import com.swiftroute.domain.repository.InventoryItemRepository;
import com.swiftroute.dto.response.ChaosTestResultResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private String dispatcherToken;
    private String techToken;

    @BeforeEach
    void obtainTokens() throws Exception {
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

        // Ensure clean, deterministic state for scarce test item
        InventoryItem item = inventoryItemRepository.findByPartNumber("SCARCE-SENSOR-CHILLER").orElseThrow();
        item.setQuantityReserved(0);
        inventoryItemRepository.saveAndFlush(item);
    }

    @Test
    @DisplayName("Pessimistic locking guarantees ZERO oversell when 10 threads race for 1 scarce sensor")
    void testConcurrentRaceConditionZeroOverselling() {
        // Run chaos simulation with 10 threads for SCARCE-SENSOR-CHILLER (stock = 1)
        ChaosTestResultResponse result = inventoryService.runChaosSimulation(10, true);

        assertEquals(10, result.getConcurrencyLevel());
        assertEquals(1, result.getSuccessfulReservations(), "Exactly 1 thread must acquire the lock and succeed");
        assertEquals(9, result.getRejectedRequests(), "Remaining 9 threads must be rejected due to insufficient stock");
        assertTrue(result.isZeroOversellGuaranteed(), "Zero oversell must be mathematically guaranteed");

        // Verify that after reset, item stock is still intact
        InventoryItem item = inventoryItemRepository.findByPartNumber("SCARCE-SENSOR-CHILLER").orElseThrow();
        assertEquals(1, item.getQuantityOnHand());
        assertEquals(0, item.getQuantityReserved());
    }

    @Test
    @DisplayName("POST /api/inventory/chaos-test endpoint executes multi-threaded race simulation")
    void testChaosTestEndpoint() throws Exception {
        mockMvc.perform(post("/api/inventory/chaos-test?threads=8&resetAfterTest=true")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.concurrencyLevel").value(8))
                .andExpect(jsonPath("$.data.successfulReservations").value(1))
                .andExpect(jsonPath("$.data.rejectedRequests").value(7))
                .andExpect(jsonPath("$.data.zeroOversellGuaranteed").value(true))
                .andExpect(jsonPath("$.data.partNumber").value("SCARCE-SENSOR-CHILLER"));
    }

    @Test
    @DisplayName("GET /api/inventory/items fetches all catalog parts with real-time stock levels")
    void testGetAllInventoryItems() throws Exception {
        mockMvc.perform(get("/api/inventory/items")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].partNumber").isNotEmpty())
                .andExpect(jsonPath("$.data[0].quantityOnHand").isNumber())
                .andExpect(jsonPath("$.data[0].availableQuantity").isNumber());
    }

    @Test
    @DisplayName("GET /api/inventory/items/low-stock identifies parts at or below reorder threshold")
    void testGetLowStockItems() throws Exception {
        mockMvc.perform(get("/api/inventory/items/low-stock")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Technician role is forbidden from reserving parts directly")
    void testTechnicianForbiddenFromReserveEndpoint() throws Exception {
        String payload = """
            {
                "jobId": 1,
                "items": [
                    { "inventoryItemId": 1, "quantity": 1 }
                ]
            }
        """;

        mockMvc.perform(post("/api/inventory/reserve")
                        .header("Authorization", "Bearer " + techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("FORBIDDEN"));
    }
}
