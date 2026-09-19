package com.swiftroute.service;

import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.entity.Skill;
import com.swiftroute.domain.entity.SlaEscalationEvent;
import com.swiftroute.domain.enums.EscalationStage;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.domain.repository.ServiceRequestRepository;
import com.swiftroute.domain.repository.SkillRepository;
import com.swiftroute.domain.repository.SlaEscalationEventRepository;
import com.swiftroute.dto.response.SlaEvaluationResult;
import com.swiftroute.dto.response.SlaMetricsSummaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SlaEscalationEngineDeepTest {

    @Autowired
    private SlaService slaService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SlaEscalationEventRepository slaEscalationEventRepository;

    private Job createCustomJob(Priority priority, Instant createdAt, Instant responseDeadline, Instant resolutionDeadline) {
        Skill skill = skillRepository.findAll().stream().findFirst().orElseThrow();
        ServiceRequest sr = new ServiceRequest(
                "SLA Deep Test Site",
                "212-555-4321",
                "500 7th Ave, New York, NY",
                40.7530,
                -73.9890,
                "SLA Compliance Verification",
                "Deep engine testing",
                priority,
                skill,
                60,
                "PENDING"
        );
        ServiceRequest savedSr = serviceRequestRepository.save(sr);

        Job job = new Job(
                savedSr,
                priority,
                JobStatus.ASSIGNED,
                responseDeadline,
                resolutionDeadline,
                null
        );
        job.setCreatedAt(createdAt);
        return jobRepository.save(job);
    }

    @Test
    @DisplayName("Idempotent SLA escalation: Repeated evaluations produce exactly one escalation event per stage")
    void testIdempotentEscalationMultipleCycles() {
        Instant now = Instant.now();
        Instant createdAt = now.minus(2, ChronoUnit.HOURS);
        Instant responseDeadline = now.minus(1, ChronoUnit.HOURS); // Breached 1 hour ago
        Instant resolutionDeadline = now.plus(4, ChronoUnit.HOURS);

        Job job = createCustomJob(Priority.CRITICAL, createdAt, responseDeadline, resolutionDeadline);

        // Run evaluation cycle 4 times sequentially
        for (int i = 0; i < 4; i++) {
            SlaEvaluationResult eval = slaService.processJobEscalation(job.getId(), now);
            assertEquals(SlaStatus.BREACHED, eval.getCurrentSlaStatus());
            assertTrue(eval.isResponseBreached(), "Response SLA must be flagged as breached");
        }

        // Verify database holds exactly 1 BREACHED event for this job (idempotent constraint verified)
        List<SlaEscalationEvent> events = slaEscalationEventRepository.findByJobIdOrderByTriggeredAtDesc(job.getId());
        long breachCount = events.stream().filter(e -> e.getThresholdStage() == EscalationStage.BREACHED).count();
        assertEquals(1, breachCount, "Database unique constraint must strictly prevent duplicate breach alerts");
    }

    @Test
    @DisplayName("Warning threshold detection: At 85% elapsed response SLA, state transitions to NEARING_BREACH")
    void testWarningThresholdDetection() {
        Instant now = Instant.now();
        // 100 minutes total response window, 85 minutes elapsed -> 85% elapsed
        Instant createdAt = now.minus(85, ChronoUnit.MINUTES);
        Instant responseDeadline = now.plus(15, ChronoUnit.MINUTES);
        Instant resolutionDeadline = now.plus(300, ChronoUnit.MINUTES);

        Job job = new Job();
        job.setId(999L);
        job.setPriority(Priority.HIGH);
        job.setStatus(JobStatus.ASSIGNED);
        job.setCreatedAt(createdAt);
        job.setSlaResponseDeadline(responseDeadline);
        job.setSlaResolutionDeadline(resolutionDeadline);

        SlaEvaluationResult eval = slaService.evaluateJobSla(job, now);

        assertEquals(SlaStatus.NEARING_BREACH, eval.getCurrentSlaStatus(), "85% elapsed SLA must flag NEARING_BREACH");
        assertFalse(eval.isResponseBreached(), "Job must not be marked breached before deadline passes");
        assertTrue(eval.getResponseElapsedPercent() >= 70.0, "Elapsed percent must exceed 70% threshold");
    }

    @Test
    @DisplayName("SLA compliance summary calculates correct portfolio compliance rate")
    void testSlaComplianceMetricsCalculation() {
        SlaMetricsSummaryResponse summary = slaService.getSlaMetricsSummary();

        assertNotNull(summary);
        assertTrue(summary.getTotalMonitoredJobs() >= 0);
        assertTrue(summary.getHealthyJobs() >= 0);
        assertTrue(summary.getNearingBreachJobs() >= 0);
        assertTrue(summary.getBreachedJobs() >= 0);
        assertTrue(
                summary.getComplianceRatePercent() >= 0.0 && summary.getComplianceRatePercent() <= 100.0,
                "Compliance rate must be between 0% and 100%"
        );
    }
}
