package com.swiftroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftroute.common.GeoUtils;
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
class DispatchRankingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    }

    @Test
    @DisplayName("Haversine formula computes accurate geodesic distance between NYC landmarks")
    void testHaversineAccuracy() {
        // Times Square (40.7580, -73.9855) to Empire State Building (40.7484, -73.9857)
        double distance = GeoUtils.haversineDistanceKm(40.7580, -73.9855, 40.7484, -73.9857);
        assertTrue(distance >= 1.0 && distance <= 1.2,
                "Expected distance ~1.07 km between Times Sq and Empire State, got: " + distance);

        // Same point distance is 0.0
        assertEquals(0.0, GeoUtils.haversineDistanceKm(40.7580, -73.9855, 40.7580, -73.9855));
    }

    @Test
    @DisplayName("Dispatch recommendation hard-filters ineligible skills and ranks closest/highest-skilled technician #1")
    void testDispatchCandidateScoringAndRanking() throws Exception {
        // Seed job 1 requires HVAC_LVL2 (Mount Sinai, 40.7900, -73.9530)
        MvcResult result = mockMvc.perform(get("/api/dispatch/recommendations/1")
                        .header("Authorization", "Bearer " + dispatcherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(1))
                .andExpect(jsonPath("$.data.requiredSkillCode").value("HVAC_LVL2"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.path("data");
        JsonNode candidates = data.path("candidates");

        // Technician 3 (Marcus) only has ELEC/PLUMB, so must be filtered out
        for (JsonNode candidate : candidates) {
            assertNotEquals("Marcus Brody", candidate.path("technicianName").asText(),
                    "Marcus must be excluded because he lacks the HVAC_LVL2 skill");
        }

        // Must have at least Dave and Elena
        assertTrue(candidates.size() >= 2, "Expected at least 2 HVAC technicians, got: " + candidates.size());

        // Dave is closer (Times Sq, ~2.7 km) and higher proficiency (Level 2) vs Elena (FiDi, Level 1)
        JsonNode topCandidate = data.path("recommendedCandidate");
        assertEquals("David Miller", topCandidate.path("technicianName").asText(),
                "David should be Rank #1 due to proximity (2.7km) and Level 2 proficiency");
        assertTrue(topCandidate.path("recommended").asBoolean());
        assertEquals(1, topCandidate.path("rank").asInt());

        // Verify score breakdown metrics
        JsonNode breakdown = topCandidate.path("scoreBreakdown");
        assertTrue(breakdown.path("totalScore").asDouble() > 0.0);
        assertTrue(breakdown.path("skillScore").asDouble() > 0.0);
        assertTrue(breakdown.path("distanceScore").asDouble() > 0.0);
        assertTrue(breakdown.path("workloadScore").asDouble() > 0.0);
        assertFalse(breakdown.path("explanation").asText().isBlank());
    }

    @Test
    @DisplayName("Technician role is forbidden from accessing dispatch candidate recommendations")
    void testTechnicianForbiddenFromRecommendations() throws Exception {
        mockMvc.perform(get("/api/dispatch/recommendations/1")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.errorCode").value("FORBIDDEN"));
    }
}
