package com.swiftroute.controller;

import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SlaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SkillRepository skillRepository;

    private Job testJob;

    @BeforeEach
    void setUp() {
        var skill = skillRepository.findAll().get(0);

        ServiceRequest sr = new ServiceRequest(
                "SLA Controller Test Customer",
                "212-555-0199",
                "500 Broadway, New York, NY",
                40.7209,
                -73.9987,
                "Controller SLA Test",
                "Verification of controller SLA endpoints",
                Priority.HIGH,
                skill,
                45,
                "PENDING"
        );
        ServiceRequest savedSr = serviceRequestRepository.save(sr);

        Job job = new Job(
                savedSr,
                Priority.HIGH,
                JobStatus.PENDING,
                Instant.now().plus(120, ChronoUnit.MINUTES),
                Instant.now().plus(480, ChronoUnit.MINUTES),
                SlaStatus.HEALTHY
        );
        this.testJob = jobRepository.save(job);
    }

    @Test
    @DisplayName("GET /api/sla/policies returns 200 with list of configured policies for DISPATCHER")
    @WithMockUser(username = "dispatcher", roles = {"DISPATCHER"})
    void testGetAllPoliciesDispatcher() throws Exception {
        mockMvc.perform(get("/api/sla/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].priority").exists())
                .andExpect(jsonPath("$.data[0].responseDeadlineMinutes").isNumber())
                .andExpect(jsonPath("$.data[0].resolutionDeadlineMinutes").isNumber());
    }

    @Test
    @DisplayName("GET /api/sla/policies returns 401 UNAUTHORIZED when called without credentials")
    void testGetAllPoliciesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/sla/policies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/sla/metrics/summary returns 200 with breakdown for ADMIN")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetMetricsSummaryAdmin() throws Exception {
        mockMvc.perform(get("/api/sla/metrics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMonitoredJobs").isNumber())
                .andExpect(jsonPath("$.data.healthyJobs").isNumber())
                .andExpect(jsonPath("$.data.nearingBreachJobs").isNumber())
                .andExpect(jsonPath("$.data.breachedJobs").isNumber())
                .andExpect(jsonPath("$.data.complianceRatePercent").isNumber());
    }

    @Test
    @DisplayName("GET /api/sla/jobs/{jobId}/escalations returns 200 for TECHNICIAN")
    @WithMockUser(username = "tech_dave", roles = {"TECHNICIAN"})
    void testGetJobEscalationsTechnician() throws Exception {
        mockMvc.perform(get("/api/sla/jobs/{jobId}/escalations", testJob.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/sla/evaluate manually triggers sweep and returns 200 for ADMIN")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testTriggerBatchEvaluationAdmin() throws Exception {
        mockMvc.perform(post("/api/sla/evaluate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("POST /api/sla/evaluate returns 403 FORBIDDEN for TECHNICIAN role")
    @WithMockUser(username = "tech_dave", roles = {"TECHNICIAN"})
    void testTriggerBatchEvaluationForbiddenForTechnician() throws Exception {
        mockMvc.perform(post("/api/sla/evaluate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/sla/jobs/{jobId}/evaluate returns 200 with evaluation result for DISPATCHER")
    @WithMockUser(username = "dispatcher", roles = {"DISPATCHER"})
    void testEvaluateSingleJobDispatcher() throws Exception {
        mockMvc.perform(post("/api/sla/jobs/{jobId}/evaluate", testJob.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(testJob.getId()))
                .andExpect(jsonPath("$.data.currentSlaStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.data.responseElapsedPercent").isNumber());
    }
}
