package com.swiftroute.service;

import com.swiftroute.common.GeoUtils;
import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.AssignmentStatus;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.TechnicianStatus;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.request.DispatchConfirmationRequest;
import com.swiftroute.dto.response.DispatchConfirmationResponse;
import com.swiftroute.dto.response.DispatchRecommendationResponse;
import com.swiftroute.dto.response.ScoreBreakdownResponse;
import com.swiftroute.dto.response.TechnicianCandidateResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.exception.InvalidStateTransitionException;
import com.swiftroute.exception.ResourceNotFoundException;
import com.swiftroute.exception.TechnicianConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DispatchService {

    public static final double WEIGHT_SKILL = 0.35;
    public static final double WEIGHT_DISTANCE = 0.40;
    public static final double WEIGHT_WORKLOAD = 0.25;
    public static final double MAX_RADIUS_KM = 50.0;

    private final JobRepository jobRepository;
    private final TechnicianRepository technicianRepository;
    private final AssignmentRepository assignmentRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private com.swiftroute.websocket.WebSocketEventPublisher webSocketEventPublisher;

    public DispatchService(JobRepository jobRepository,
                           TechnicianRepository technicianRepository,
                           AssignmentRepository assignmentRepository,
                           AuditEventRepository auditEventRepository,
                           UserRepository userRepository,
                           ServiceRequestRepository serviceRequestRepository) {
        this.jobRepository = jobRepository;
        this.technicianRepository = technicianRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setWebSocketEventPublisher(com.swiftroute.websocket.WebSocketEventPublisher webSocketEventPublisher) {
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    @Transactional(readOnly = true)
    public DispatchRecommendationResponse getRecommendationsForJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        ServiceRequest request = job.getServiceRequest();
        Skill skill = request.getRequiredSkill();
        double jobLat = request.getLatitude();
        double jobLon = request.getLongitude();

        // 1. Hard Filter: Skill match and not OFF_DUTY
        List<Technician> eligibleTechnicians = technicianRepository.findEligibleTechniciansBySkill(skill.getId());

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<TechnicianCandidateResponse> candidates = new ArrayList<>();

        for (Technician tech : eligibleTechnicians) {
            // 2. Hard Filter: Daily capacity
            long activeJobsToday = assignmentRepository.countTechnicianActiveJobsSince(tech.getId(), startOfDay);
            if (activeJobsToday >= tech.getMaxDailyJobs()) {
                continue; // Capacity exhausted
            }

            // 3. Multi-factor scoring
            int proficiency = extractProficiency(tech, skill.getId());
            double skillScore = computeSkillScore(proficiency);

            double distanceKm = GeoUtils.haversineDistanceKm(tech.getCurrentLatitude(), tech.getCurrentLongitude(), jobLat, jobLon);
            double distanceScore = computeDistanceScore(distanceKm);

            double workloadScore = computeWorkloadScore(activeJobsToday, tech.getMaxDailyJobs(), tech.getStatus());

            double rawTotal = (WEIGHT_SKILL * skillScore) + (WEIGHT_DISTANCE * distanceScore) + (WEIGHT_WORKLOAD * workloadScore);
            double totalScore = Math.round(rawTotal * 10.0) / 10.0;

            String explanation = String.format(
                    "Proficiency Level %d (%.0f pts) • %.1f km away (%.1f pts) • %d/%d daily jobs (%.1f pts)",
                    proficiency, skillScore, distanceKm, distanceScore, activeJobsToday, tech.getMaxDailyJobs(), workloadScore
            );

            ScoreBreakdownResponse breakdown = new ScoreBreakdownResponse(
                    Math.round(skillScore * 10.0) / 10.0,
                    Math.round(distanceScore * 10.0) / 10.0,
                    Math.round(workloadScore * 10.0) / 10.0,
                    totalScore,
                    explanation
            );

            TechnicianCandidateResponse candidate = new TechnicianCandidateResponse(
                    tech.getId(),
                    tech.getName(),
                    tech.getPhone(),
                    tech.getStatus(),
                    tech.getCurrentLatitude(),
                    tech.getCurrentLongitude(),
                    proficiency,
                    distanceKm,
                    activeJobsToday,
                    tech.getMaxDailyJobs(),
                    0, // will rank after sorting
                    false,
                    breakdown
            );
            candidates.add(candidate);
        }

        // 4. Sort candidates descending by totalScore, then ascending by distanceKm
        candidates.sort(Comparator
                .comparingDouble((TechnicianCandidateResponse c) -> c.getScoreBreakdown().getTotalScore())
                .reversed()
                .thenComparingDouble(TechnicianCandidateResponse::getDistanceKm));

        // 5. Assign ranks and mark #1 as recommended
        for (int i = 0; i < candidates.size(); i++) {
            TechnicianCandidateResponse c = candidates.get(i);
            c.setRank(i + 1);
            if (i == 0) {
                c.setRecommended(true);
            }
        }

        TechnicianCandidateResponse topCandidate = candidates.isEmpty() ? null : candidates.get(0);

        return new DispatchRecommendationResponse(
                job.getId(),
                request.getId(),
                skill.getCode(),
                skill.getName(),
                jobLat,
                jobLon,
                candidates.size(),
                topCandidate,
                candidates
        );
    }

    private int extractProficiency(Technician tech, Long skillId) {
        if (tech.getTechnicianSkills() != null) {
            for (TechnicianSkill ts : tech.getTechnicianSkills()) {
                if (ts.getSkill().getId().equals(skillId)) {
                    return ts.getProficiencyLevel() != null ? ts.getProficiencyLevel() : 1;
                }
            }
        }
        return 1;
    }

    private double computeSkillScore(int proficiency) {
        return switch (proficiency) {
            case 3 -> 100.0;
            case 2 -> 85.0;
            case 1 -> 70.0;
            default -> 60.0;
        };
    }

    private double computeDistanceScore(double distanceKm) {
        if (distanceKm <= 2.0) {
            return 100.0;
        }
        if (distanceKm >= MAX_RADIUS_KM) {
            return 0.0;
        }
        return Math.max(0.0, 100.0 * (1.0 - (distanceKm / MAX_RADIUS_KM)));
    }

    private double computeWorkloadScore(long activeJobs, int maxJobs, TechnicianStatus status) {
        double capacityRatio = 1.0 - ((double) activeJobs / Math.max(1, maxJobs));
        double baseScore = Math.max(0.0, 100.0 * capacityRatio);

        // 25% penalty if currently on an ongoing job
        if (status == TechnicianStatus.BUSY) {
            baseScore *= 0.75;
        }

        return baseScore;
    }

    @Transactional
    public DispatchConfirmationResponse confirmDispatch(DispatchConfirmationRequest request, String actorUsername) {
        if (!request.getScheduledEndTime().isAfter(request.getScheduledStartTime())) {
            throw new BusinessRuleException("Scheduled end time must be after scheduled start time");
        }

        // 1. Lock job pessimistically to prevent concurrent assignments
        Job job = jobRepository.findByIdWithPessimisticLock(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", request.getJobId()));

        // 2. Validate state machine invariant: job must be PENDING
        if (job.getStatus() != JobStatus.PENDING) {
            throw new InvalidStateTransitionException("Job", job.getStatus().name(), JobStatus.ASSIGNED.name());
        }

        // 3. Validate technician eligibility
        Technician technician = technicianRepository.findById(request.getSelectedTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", request.getSelectedTechnicianId()));

        if (technician.getStatus() == TechnicianStatus.OFF_DUTY) {
            throw new BusinessRuleException("Technician " + technician.getName() + " is OFF_DUTY and cannot receive assignments");
        }

        Skill requiredSkill = job.getServiceRequest().getRequiredSkill();
        boolean hasSkill = false;
        if (technician.getTechnicianSkills() != null) {
            for (TechnicianSkill ts : technician.getTechnicianSkills()) {
                if (ts.getSkill().getId().equals(requiredSkill.getId())) {
                    hasSkill = true;
                    break;
                }
            }
        }
        if (!hasSkill) {
            throw new BusinessRuleException("Technician " + technician.getName() + " lacks required skill: " + requiredSkill.getName() + " (" + requiredSkill.getCode() + ")");
        }

        // 4. Overlap invariant: Check if technician has overlapping active assignments
        List<Assignment> overlaps = assignmentRepository.findOverlappingAssignments(
                technician.getId(),
                request.getScheduledStartTime(),
                request.getScheduledEndTime()
        );
        if (!overlaps.isEmpty()) {
            throw new TechnicianConflictException(technician.getId(), request.getScheduledStartTime(), request.getScheduledEndTime());
        }

        // 5. Capacity invariant: Daily job capacity limit
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long activeJobsToday = assignmentRepository.countTechnicianActiveJobsSince(technician.getId(), startOfDay);
        if (activeJobsToday >= technician.getMaxDailyJobs()) {
            throw new BusinessRuleException("Technician " + technician.getName() + " has reached the daily limit of " + technician.getMaxDailyJobs() + " jobs");
        }

        // 6. Multi-factor recommendation evaluation & manual override detection
        DispatchRecommendationResponse recommendations = getRecommendationsForJob(job.getId());
        TechnicianCandidateResponse topCandidate = recommendations.getRecommendedCandidate();

        boolean isOverride = topCandidate != null && !topCandidate.getTechnicianId().equals(technician.getId());
        String overrideReason = request.getOverrideReason();

        if (isOverride) {
            if (overrideReason == null || overrideReason.trim().isEmpty()) {
                throw new BusinessRuleException(String.format(
                        "Manual override detected: Selected technician '%s' is not the #1 recommendation ('%s'). An overrideReason is mandatory.",
                        technician.getName(),
                        topCandidate != null ? topCandidate.getTechnicianName() : "None"
                ));
            }

            // Immutable Audit Log for Manual Override
            String overridePayload = String.format(
                    "{\"jobId\":%d,\"selectedTechnicianId\":%d,\"selectedTechnicianName\":\"%s\",\"recommendedTechnicianId\":%s,\"recommendedTechnicianName\":\"%s\",\"overrideReason\":\"%s\"}",
                    job.getId(),
                    technician.getId(),
                    technician.getName(),
                    topCandidate != null ? topCandidate.getTechnicianId().toString() : "null",
                    topCandidate != null ? topCandidate.getTechnicianName() : "None",
                    overrideReason.replace("\"", "\\\"")
            );
            AuditEvent overrideEvent = new AuditEvent(
                    "JOB",
                    job.getId(),
                    AuditAction.MANUAL_OVERRIDE,
                    actorUsername != null ? actorUsername : "dispatcher",
                    overridePayload
            );
            auditEventRepository.save(overrideEvent);
        }

        // 7. Transition Job and ServiceRequest state
        job.setStatus(JobStatus.ASSIGNED);
        job.setScheduledStart(request.getScheduledStartTime());
        job.setScheduledEnd(request.getScheduledEndTime());
        Job savedJob = jobRepository.save(job);

        if (savedJob.getServiceRequest() != null) {
            savedJob.getServiceRequest().setStatus("ASSIGNED");
            serviceRequestRepository.save(savedJob.getServiceRequest());
        }

        // 8. Create and persist Assignment
        User actor = actorUsername != null ? userRepository.findByUsername(actorUsername).orElse(null) : null;
        Assignment assignment = new Assignment(
                savedJob,
                technician,
                actor,
                AssignmentStatus.ACTIVE,
                request.getScheduledStartTime(),
                request.getScheduledEndTime(),
                request.getNotes()
        );
        Assignment savedAssignment = assignmentRepository.save(assignment);

        // 9. Immutable Audit Log for Dispatch
        String dispatchPayload = String.format(
                "{\"assignmentId\":%d,\"technicianId\":%d,\"technicianName\":\"%s\",\"scheduledStartTime\":\"%s\",\"scheduledEndTime\":\"%s\",\"manualOverride\":%b}",
                savedAssignment.getId(),
                technician.getId(),
                technician.getName(),
                request.getScheduledStartTime(),
                request.getScheduledEndTime(),
                isOverride
        );
        AuditEvent dispatchEvent = new AuditEvent(
                "JOB",
                savedJob.getId(),
                AuditAction.DISPATCHED,
                actorUsername != null ? actorUsername : "dispatcher",
                dispatchPayload
        );
        auditEventRepository.save(dispatchEvent);

        if (webSocketEventPublisher != null) {
            webSocketEventPublisher.publishJobEvent("JOB_ASSIGNED", java.util.Map.of(
                    "jobId", savedJob.getId(),
                    "technicianId", technician.getId(),
                    "technicianName", technician.getName(),
                    "scheduledStartTime", request.getScheduledStartTime().toString(),
                    "scheduledEndTime", request.getScheduledEndTime().toString(),
                    "isOverride", isOverride,
                    "timestamp", java.time.Instant.now().toString()
            ));
        }

        return new DispatchConfirmationResponse(
                savedAssignment.getId(),
                savedJob.getId(),
                technician.getId(),
                technician.getName(),
                savedJob.getStatus(),
                savedAssignment.getScheduledStartTime(),
                savedAssignment.getScheduledEndTime(),
                isOverride,
                isOverride ? overrideReason : null,
                actorUsername != null ? actorUsername : "dispatcher",
                savedAssignment.getAssignedAt()
        );
    }
}
