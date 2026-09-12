package com.swiftroute.service;

import com.swiftroute.common.GeoUtils;
import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.TechnicianStatus;
import com.swiftroute.domain.repository.AssignmentRepository;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.domain.repository.TechnicianRepository;
import com.swiftroute.dto.response.DispatchRecommendationResponse;
import com.swiftroute.dto.response.ScoreBreakdownResponse;
import com.swiftroute.dto.response.TechnicianCandidateResponse;
import com.swiftroute.exception.ResourceNotFoundException;
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

    public DispatchService(JobRepository jobRepository,
                           TechnicianRepository technicianRepository,
                           AssignmentRepository assignmentRepository) {
        this.jobRepository = jobRepository;
        this.technicianRepository = technicianRepository;
        this.assignmentRepository = assignmentRepository;
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
}
