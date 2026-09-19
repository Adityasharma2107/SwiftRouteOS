package com.swiftroute.service;

import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.SlaEscalationEvent;
import com.swiftroute.domain.entity.SlaPolicy;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.EscalationStage;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.SlaStatus;
import com.swiftroute.domain.repository.AuditEventRepository;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.domain.repository.SlaEscalationEventRepository;
import com.swiftroute.domain.repository.SlaPolicyRepository;
import com.swiftroute.dto.response.SlaEscalationEventResponse;
import com.swiftroute.dto.response.SlaEvaluationResult;
import com.swiftroute.dto.response.SlaMetricsSummaryResponse;
import com.swiftroute.dto.response.SlaPolicyResponse;
import com.swiftroute.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swiftroute.websocket.WebSocketEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private final JobRepository jobRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaEscalationEventRepository slaEscalationEventRepository;
    private final AuditEventRepository auditEventRepository;
    private WebSocketEventPublisher webSocketEventPublisher;

    @Value("${swiftroute.sla.warning-threshold-percent:0.70}")
    private double warningThresholdPercent;

    public SlaService(JobRepository jobRepository,
                      SlaPolicyRepository slaPolicyRepository,
                      SlaEscalationEventRepository slaEscalationEventRepository,
                      AuditEventRepository auditEventRepository) {
        this.jobRepository = jobRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.slaEscalationEventRepository = slaEscalationEventRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Autowired(required = false)
    public void setWebSocketEventPublisher(WebSocketEventPublisher webSocketEventPublisher) {
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    /**
     * Pure evaluation of a Job's current SLA compliance, calculating elapsed percentages,
     * remaining time windows, and threshold crossing status without mutating state.
     */
    public SlaEvaluationResult evaluateJobSla(Job job, Instant now) {
        Instant createdAt = job.getCreatedAt() != null ? job.getCreatedAt() : now;
        Instant responseDeadline = job.getSlaResponseDeadline() != null
                ? job.getSlaResponseDeadline()
                : createdAt.plus(Duration.ofHours(2));
        Instant resolutionDeadline = job.getSlaResolutionDeadline() != null
                ? job.getSlaResolutionDeadline()
                : createdAt.plus(Duration.ofHours(8));

        // 1. Response SLA evaluation
        long totalResponseMs = Math.max(1, Duration.between(createdAt, responseDeadline).toMillis());
        boolean responseBreached;
        double responseElapsedPercent;
        long responseRemainingSec;

        if (job.getActualResponseAt() != null) {
            // Already responded
            responseBreached = job.getActualResponseAt().isAfter(responseDeadline);
            long actualMs = Math.max(0, Duration.between(createdAt, job.getActualResponseAt()).toMillis());
            responseElapsedPercent = Math.min(100.0, (double) actualMs / totalResponseMs * 100.0);
            responseRemainingSec = 0;
        } else {
            // Awaiting response
            responseBreached = now.isAfter(responseDeadline);
            long elapsedMs = Math.max(0, Duration.between(createdAt, now).toMillis());
            responseElapsedPercent = Math.min(200.0, (double) elapsedMs / totalResponseMs * 100.0);
            responseRemainingSec = Math.max(0, Duration.between(now, responseDeadline).toSeconds());
        }

        // 2. Resolution SLA evaluation
        long totalResolutionMs = Math.max(1, Duration.between(createdAt, resolutionDeadline).toMillis());
        boolean resolutionBreached;
        double resolutionElapsedPercent;
        long resolutionRemainingSec;

        if (job.getActualResolutionAt() != null) {
            // Already resolved
            resolutionBreached = job.getActualResolutionAt().isAfter(resolutionDeadline);
            long actualMs = Math.max(0, Duration.between(createdAt, job.getActualResolutionAt()).toMillis());
            resolutionElapsedPercent = Math.min(100.0, (double) actualMs / totalResolutionMs * 100.0);
            resolutionRemainingSec = 0;
        } else {
            // Awaiting resolution
            resolutionBreached = now.isAfter(resolutionDeadline);
            long elapsedMs = Math.max(0, Duration.between(createdAt, now).toMillis());
            resolutionElapsedPercent = Math.min(200.0, (double) elapsedMs / totalResolutionMs * 100.0);
            resolutionRemainingSec = Math.max(0, Duration.between(now, resolutionDeadline).toSeconds());
        }

        // 3. Determine composite status
        SlaStatus computedStatus;
        if (responseBreached || resolutionBreached) {
            computedStatus = SlaStatus.BREACHED;
        } else {
            double warningThreshold = warningThresholdPercent * 100.0;
            boolean nearingResponse = (job.getActualResponseAt() == null && responseElapsedPercent >= warningThreshold);
            boolean nearingResolution = (job.getActualResolutionAt() == null && resolutionElapsedPercent >= warningThreshold);

            if (nearingResponse || nearingResolution) {
                computedStatus = SlaStatus.NEARING_BREACH;
            } else {
                computedStatus = SlaStatus.HEALTHY;
            }
        }

        return new SlaEvaluationResult(
                job.getId(),
                job.getPriority(),
                computedStatus,
                Math.round(responseElapsedPercent * 10.0) / 10.0,
                Math.round(resolutionElapsedPercent * 10.0) / 10.0,
                responseRemainingSec,
                resolutionRemainingSec,
                responseBreached,
                resolutionBreached,
                now
        );
    }

    /**
     * Idempotently evaluates a job's SLA and logs escalation events if threshold stages are crossed.
     * Guaranteed idempotent by database unique constraint (job_id, threshold_stage).
     */
    @Transactional
    public SlaEvaluationResult processJobEscalation(Long jobId, Instant now) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        return processJobEscalation(job, now);
    }

    @Transactional
    public SlaEvaluationResult processJobEscalation(Job job, Instant now) {
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            return evaluateJobSla(job, now);
        }

        SlaEvaluationResult eval = evaluateJobSla(job, now);
        SlaStatus computed = eval.getCurrentSlaStatus();

        if (computed == SlaStatus.BREACHED) {
            // Check idempotency guard
            if (!slaEscalationEventRepository.existsByJobIdAndThresholdStage(job.getId(), EscalationStage.BREACHED)) {
                String details = String.format(
                        "{\"stage\":\"BREACHED\",\"responseBreached\":%b,\"resolutionBreached\":%b,\"evaluatedAt\":\"%s\"}",
                        eval.isResponseBreached(), eval.isResolutionBreached(), now
                );
                SlaEscalationEvent event = new SlaEscalationEvent(job, EscalationStage.BREACHED, details);
                slaEscalationEventRepository.save(event);

                job.setSlaStatus(SlaStatus.BREACHED);
                jobRepository.save(job);

                AuditEvent auditEvent = new AuditEvent(
                        "JOB",
                        job.getId(),
                        AuditAction.ESCALATED,
                        "sla-engine",
                        details
                );
                auditEventRepository.save(auditEvent);
                log.warn("SLA BREACHED for Job #{} (Priority: {})", job.getId(), job.getPriority());

                if (webSocketEventPublisher != null) {
                    webSocketEventPublisher.publishSlaAlert("SLA_BREACHED", Map.of(
                            "jobId", job.getId(),
                            "priority", job.getPriority().name(),
                            "stage", "BREACHED",
                            "timestamp", now.toString()
                    ));
                }
            }
        } else if (computed == SlaStatus.NEARING_BREACH) {
            // Check idempotency guard
            if (!slaEscalationEventRepository.existsByJobIdAndThresholdStage(job.getId(), EscalationStage.NEARING_BREACH)) {
                String details = String.format(
                        "{\"stage\":\"NEARING_BREACH\",\"responseElapsedPercent\":%.1f,\"resolutionElapsedPercent\":%.1f,\"evaluatedAt\":\"%s\"}",
                        eval.getResponseElapsedPercent(), eval.getResolutionElapsedPercent(), now
                );
                SlaEscalationEvent event = new SlaEscalationEvent(job, EscalationStage.NEARING_BREACH, details);
                slaEscalationEventRepository.save(event);

                job.setSlaStatus(SlaStatus.NEARING_BREACH);
                jobRepository.save(job);

                AuditEvent auditEvent = new AuditEvent(
                        "JOB",
                        job.getId(),
                        AuditAction.ESCALATED,
                        "sla-engine",
                        details
                );
                auditEventRepository.save(auditEvent);
                log.info("SLA NEARING_BREACH warning triggered for Job #{}", job.getId());

                if (webSocketEventPublisher != null) {
                    webSocketEventPublisher.publishSlaAlert("SLA_WARNING", Map.of(
                            "jobId", job.getId(),
                            "priority", job.getPriority().name(),
                            "stage", "NEARING_BREACH",
                            "responseElapsedPercent", eval.getResponseElapsedPercent(),
                            "resolutionElapsedPercent", eval.getResolutionElapsedPercent(),
                            "timestamp", now.toString()
                    ));
                }
            }
        }

        return eval;
    }

    /**
     * Evaluates all active jobs currently being monitored for SLA compliance.
     */
    @Transactional
    public int evaluateAndEscalateActiveJobs() {
        List<Job> activeJobs = jobRepository.findActiveJobsForSlaMonitoring();
        Instant now = Instant.now();
        int count = 0;

        for (Job job : activeJobs) {
            processJobEscalation(job, now);
            count++;
        }

        return count;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {
        return slaPolicyRepository.findAll().stream()
                .map(p -> new SlaPolicyResponse(
                        p.getId(),
                        p.getPriority(),
                        p.getResponseDeadlineMinutes(),
                        p.getResolutionDeadlineMinutes()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SlaEscalationEventResponse> getEscalationEventsForJob(Long jobId) {
        return slaEscalationEventRepository.findByJobIdOrderByTriggeredAtDesc(jobId).stream()
                .map(e -> new SlaEscalationEventResponse(
                        e.getId(),
                        e.getJob().getId(),
                        e.getThresholdStage(),
                        e.getTriggeredAt(),
                        e.getDetails()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SlaMetricsSummaryResponse getSlaMetricsSummary() {
        long totalActive = jobRepository.countByStatusNotIn(List.of(JobStatus.COMPLETED, JobStatus.CANCELLED));
        long healthy = jobRepository.countBySlaStatus(SlaStatus.HEALTHY);
        long nearing = jobRepository.countBySlaStatus(SlaStatus.NEARING_BREACH);
        long breached = jobRepository.countBySlaStatus(SlaStatus.BREACHED);

        double complianceRate = totalActive > 0
                ? Math.max(0.0, ((double) (totalActive - breached) / totalActive) * 100.0)
                : 100.0;
        complianceRate = Math.round(complianceRate * 10.0) / 10.0;

        return new SlaMetricsSummaryResponse(
                totalActive,
                healthy,
                nearing,
                breached,
                complianceRate
        );
    }
}
