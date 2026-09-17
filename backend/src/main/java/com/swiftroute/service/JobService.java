package com.swiftroute.service;

import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.*;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.request.JobTransitionRequest;
import com.swiftroute.dto.response.JobPartResponse;
import com.swiftroute.dto.response.JobResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.exception.InvalidStateTransitionException;
import com.swiftroute.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final AssignmentRepository assignmentRepository;
    private final TechnicianRepository technicianRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public JobService(JobRepository jobRepository,
                      AssignmentRepository assignmentRepository,
                      TechnicianRepository technicianRepository,
                      ServiceRequestRepository serviceRequestRepository,
                      AuditEventRepository auditEventRepository,
                      UserRepository userRepository,
                      InventoryService inventoryService) {
        this.jobRepository = jobRepository;
        this.assignmentRepository = assignmentRepository;
        this.technicianRepository = technicianRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    public boolean isValidTransition(JobStatus current, JobStatus target) {
        if (current == target) return true;
        return switch (current) {
            case PENDING -> target == JobStatus.ASSIGNED || target == JobStatus.CANCELLED;
            case ASSIGNED -> target == JobStatus.EN_ROUTE || target == JobStatus.CANCELLED;
            case EN_ROUTE -> target == JobStatus.IN_PROGRESS || target == JobStatus.CANCELLED;
            case IN_PROGRESS -> target == JobStatus.COMPLETED || target == JobStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    @Transactional
    public JobResponse transitionJob(Long jobId, JobTransitionRequest request, String actorUsername) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        JobStatus currentStatus = job.getStatus();
        JobStatus targetStatus = request.getTargetStatus();

        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidStateTransitionException("Job", currentStatus.name(), targetStatus.name());
        }

        // Verify technician authorization if actor is a technician
        User actor = userRepository.findByUsername(actorUsername).orElse(null);
        Assignment activeAssignment = assignmentRepository.findByJobIdAndStatus(jobId, AssignmentStatus.ACTIVE).orElse(null);

        if (actor != null && actor.getRole() == Role.ROLE_TECHNICIAN) {
            if (activeAssignment == null || !activeAssignment.getTechnician().getUser().getId().equals(actor.getId())) {
                throw new BusinessRuleException("Technicians can only update status for jobs actively assigned to them");
            }
        }

        Instant now = Instant.now();

        // Operational side effects of state transition
        switch (targetStatus) {
            case EN_ROUTE -> {
                // Technician acknowledged job and is in transit
            }
            case IN_PROGRESS -> {
                // Technician arrived on-site and started work: record actual response time
                if (job.getActualResponseAt() == null) {
                    job.setActualResponseAt(now);
                }
                if (activeAssignment != null) {
                    Technician tech = activeAssignment.getTechnician();
                    tech.setStatus(TechnicianStatus.BUSY);
                    technicianRepository.save(tech);
                }
            }
            case COMPLETED -> {
                // Work completed: record actual resolution time and release technician to AVAILABLE
                job.setActualResolutionAt(now);
                if (activeAssignment != null) {
                    activeAssignment.setStatus(AssignmentStatus.COMPLETED);
                    activeAssignment.setCompletedAt(now);
                    assignmentRepository.save(activeAssignment);

                    Technician tech = activeAssignment.getTechnician();
                    tech.setStatus(TechnicianStatus.AVAILABLE);
                    technicianRepository.save(tech);
                }
                if (job.getServiceRequest() != null) {
                    job.getServiceRequest().setStatus("COMPLETED");
                    serviceRequestRepository.save(job.getServiceRequest());
                }
                // Permanently consume reserved inventory upon completion
                inventoryService.consumeReservationsForJob(job.getId(), actorUsername);
            }
            case CANCELLED -> {
                // Job cancelled: free up technician if assigned
                if (activeAssignment != null) {
                    activeAssignment.setStatus(AssignmentStatus.CANCELLED);
                    assignmentRepository.save(activeAssignment);

                    Technician tech = activeAssignment.getTechnician();
                    tech.setStatus(TechnicianStatus.AVAILABLE);
                    technicianRepository.save(tech);
                }
                if (job.getServiceRequest() != null) {
                    job.getServiceRequest().setStatus("CANCELLED");
                    serviceRequestRepository.save(job.getServiceRequest());
                }
                // Automatic inventory rollback: release reserved stock
                inventoryService.releaseReservationsForJob(job.getId(), "Job cancelled", actorUsername);
            }
            default -> {}
        }

        job.setStatus(targetStatus);
        Job savedJob = jobRepository.save(job);

        // Record immutable audit event
        String payloadJson = String.format(
                "{\"fromStatus\":\"%s\",\"toStatus\":\"%s\",\"notes\":\"%s\"}",
                currentStatus, targetStatus, request.getNotes() != null ? request.getNotes() : ""
        );
        AuditEvent auditEvent = new AuditEvent(
                "JOB",
                savedJob.getId(),
                AuditAction.STATUS_CHANGED,
                actorUsername != null ? actorUsername : "system",
                payloadJson
        );
        auditEventRepository.save(auditEvent);

        return mapToJobResponse(savedJob, activeAssignment);
    }

    private JobResponse mapToJobResponse(Job job, Assignment assignment) {
        List<JobPartResponse> partResponses = new ArrayList<>();
        if (job.getRequiredParts() != null) {
            for (JobPart part : job.getRequiredParts()) {
                InventoryItem item = part.getInventoryItem();
                partResponses.add(new JobPartResponse(
                        part.getId(),
                        item.getId(),
                        item.getPartNumber(),
                        item.getName(),
                        part.getQuantityRequired()
                ));
            }
        }

        Long techId = assignment != null ? assignment.getTechnician().getId() : null;
        String techName = assignment != null ? assignment.getTechnician().getName() : null;

        return new JobResponse(
                job.getId(),
                job.getServiceRequest() != null ? job.getServiceRequest().getId() : null,
                job.getPriority(),
                job.getStatus(),
                job.getSlaResponseDeadline(),
                job.getSlaResolutionDeadline(),
                job.getSlaStatus(),
                job.getActualResponseAt(),
                job.getActualResolutionAt(),
                job.getScheduledStart(),
                job.getScheduledEnd(),
                techId,
                techName,
                partResponses,
                job.getCreatedAt()
        );
    }
}
