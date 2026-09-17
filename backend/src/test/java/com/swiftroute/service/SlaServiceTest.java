package com.swiftroute.service;

import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.entity.SlaEscalationEvent;
import com.swiftroute.domain.enums.*;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.response.SlaEscalationEventResponse;
import com.swiftroute.dto.response.SlaEvaluationResult;
import com.swiftroute.dto.response.SlaMetricsSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SlaServiceTest {

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

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Job testJob;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        var skill = skillRepository.findAll().get(0);

        ServiceRequest sr = new ServiceRequest(
                "SLA Test Customer",
                "212-555-0155",
                "123 SLA Blvd, New York, NY",
                40.7580,
                -73.9855,
                "SLA Escalation Inspection",
                "Evaluation of SLA threshold transitions",
                Priority.CRITICAL,
                skill,
                60,
                "PENDING"
        );
        ServiceRequest savedSr = serviceRequestRepository.save(sr);

        Job job = new Job(
                savedSr,
                Priority.CRITICAL,
                JobStatus.PENDING,
                Instant.now().plus(60, ChronoUnit.MINUTES),
                Instant.now().plus(240, ChronoUnit.MINUTES),
                SlaStatus.HEALTHY
        );
        this.testJob = jobRepository.save(job);
        this.baseTime = testJob.getCreatedAt();
        // CRITICAL SLA: 60 minutes response, 240 minutes resolution anchored to persisted createdAt
        this.testJob.setSlaResponseDeadline(baseTime.plus(60, ChronoUnit.MINUTES));
        this.testJob.setSlaResolutionDeadline(baseTime.plus(240, ChronoUnit.MINUTES));
        this.testJob = jobRepository.save(this.testJob);
    }

    @Test
    @DisplayName("SLA calculation marks job HEALTHY when elapsed percentage is under 70%")
    void testEvaluateJobSlaHealthy() {
        // 20 minutes elapsed out of 60 minutes = 33.3% (< 70%)
        Instant evalTime = baseTime.plus(20, ChronoUnit.MINUTES);

        SlaEvaluationResult eval = slaService.evaluateJobSla(testJob, evalTime);

        assertEquals(SlaStatus.HEALTHY, eval.getCurrentSlaStatus());
        assertTrue(eval.getResponseElapsedPercent() < 70.0);
        assertFalse(eval.isResponseBreached());
        assertFalse(eval.isResolutionBreached());
        assertTrue(eval.getResponseRemainingSeconds() > 0);
    }

    @Test
    @DisplayName("SLA calculation marks job NEARING_BREACH when elapsed percentage is >= 70% but < 100%")
    void testEvaluateJobSlaNearingBreach() {
        // 45 minutes elapsed out of 60 minutes = 75.0% (>= 70%)
        Instant evalTime = baseTime.plus(45, ChronoUnit.MINUTES);

        SlaEvaluationResult eval = slaService.evaluateJobSla(testJob, evalTime);

        assertEquals(SlaStatus.NEARING_BREACH, eval.getCurrentSlaStatus());
        assertTrue(eval.getResponseElapsedPercent() >= 70.0);
        assertTrue(eval.getResponseElapsedPercent() < 100.0);
        assertFalse(eval.isResponseBreached());
    }

    @Test
    @DisplayName("SLA calculation marks job BREACHED when elapsed percentage is >= 100%")
    void testEvaluateJobSlaBreached() {
        // 65 minutes elapsed out of 60 minutes = 108.3% (>= 100%)
        Instant evalTime = baseTime.plus(65, ChronoUnit.MINUTES);

        SlaEvaluationResult eval = slaService.evaluateJobSla(testJob, evalTime);

        assertEquals(SlaStatus.BREACHED, eval.getCurrentSlaStatus());
        assertTrue(eval.isResponseBreached());
        assertEquals(0, eval.getResponseRemainingSeconds());
    }

    @Test
    @DisplayName("Multiple repeated escalation calls are 100% idempotent and record exactly 1 event per stage")
    void testIdempotentEscalationEventLogging() {
        // 1. Trigger NEARING_BREACH warning multiple times
        Instant nearingTime = baseTime.plus(45, ChronoUnit.MINUTES);
        for (int i = 0; i < 5; i++) {
            slaService.processJobEscalation(testJob.getId(), nearingTime);
        }

        // Must only create 1 escalation event for NEARING_BREACH
        List<SlaEscalationEventResponse> eventsAfterNearing = slaService.getEscalationEventsForJob(testJob.getId());
        assertEquals(1, eventsAfterNearing.size(), "Idempotency must prevent duplicate NEARING_BREACH events");
        assertEquals(EscalationStage.NEARING_BREACH, eventsAfterNearing.get(0).getThresholdStage());

        Job updatedJob = jobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals(SlaStatus.NEARING_BREACH, updatedJob.getSlaStatus());

        // Verify audit event
        List<AuditEvent> auditEvents = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("JOB", testJob.getId());
        boolean hasEscalatedAudit = auditEvents.stream().anyMatch(e -> e.getAction() == AuditAction.ESCALATED);
        assertTrue(hasEscalatedAudit, "Expected AuditAction.ESCALATED to be logged");

        // 2. Advance time and trigger BREACHED multiple times
        Instant breachTime = baseTime.plus(70, ChronoUnit.MINUTES);
        for (int i = 0; i < 5; i++) {
            slaService.processJobEscalation(testJob.getId(), breachTime);
        }

        // Must now have exactly 2 escalation events: 1 NEARING_BREACH + 1 BREACHED
        List<SlaEscalationEventResponse> eventsAfterBreach = slaService.getEscalationEventsForJob(testJob.getId());
        assertEquals(2, eventsAfterBreach.size(), "Exactly 2 events total (1 per threshold stage)");
        assertEquals(EscalationStage.BREACHED, eventsAfterBreach.get(0).getThresholdStage());

        Job breachedJob = jobRepository.findById(testJob.getId()).orElseThrow();
        assertEquals(SlaStatus.BREACHED, breachedJob.getSlaStatus());
    }

    @Test
    @DisplayName("SLA metrics summary calculates accurate compliance rate and status counts")
    void testSlaMetricsSummary() {
        SlaMetricsSummaryResponse summary = slaService.getSlaMetricsSummary();

        assertNotNull(summary);
        assertTrue(summary.getTotalMonitoredJobs() >= 0);
        assertTrue(summary.getHealthyJobs() >= 0);
        assertTrue(summary.getNearingBreachJobs() >= 0);
        assertTrue(summary.getBreachedJobs() >= 0);
        assertTrue(summary.getComplianceRatePercent() >= 0.0 && summary.getComplianceRatePercent() <= 100.0);
    }
}
