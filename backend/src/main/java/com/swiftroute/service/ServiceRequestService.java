package com.swiftroute.service;

import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.request.CreateServiceRequestDto;
import com.swiftroute.dto.request.JobPartRequirementDto;
import com.swiftroute.dto.response.JobPartResponse;
import com.swiftroute.dto.response.JobResponse;
import com.swiftroute.dto.response.ServiceRequestResponse;
import com.swiftroute.dto.response.SkillResponse;
import com.swiftroute.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final JobPartRepository jobPartRepository;
    private final AuditEventRepository auditEventRepository;
    private final AssignmentRepository assignmentRepository;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 JobRepository jobRepository,
                                 SkillRepository skillRepository,
                                 SlaPolicyRepository slaPolicyRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 JobPartRepository jobPartRepository,
                                 AuditEventRepository auditEventRepository,
                                 AssignmentRepository assignmentRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.jobPartRepository = jobPartRepository;
        this.auditEventRepository = auditEventRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public ServiceRequestResponse createServiceRequest(CreateServiceRequestDto dto, String actorUsername) {
        Skill skill = skillRepository.findById(dto.getRequiredSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", dto.getRequiredSkillId()));

        // 1. Create and persist ServiceRequest
        ServiceRequest request = new ServiceRequest(
                dto.getCustomerName(),
                dto.getCustomerPhone(),
                dto.getServiceAddress(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getPriority(),
                skill,
                dto.getEstimatedDurationMinutes(),
                "PENDING"
        );
        ServiceRequest savedRequest = serviceRequestRepository.save(request);

        // 2. Compute SLA response and resolution deadlines from policy
        SlaPolicy slaPolicy = slaPolicyRepository.findByPriority(dto.getPriority())
                .orElseGet(() -> new SlaPolicy(dto.getPriority(), 60, 240));

        Instant now = Instant.now();
        Instant responseDeadline = now.plus(slaPolicy.getResponseDeadlineMinutes(), ChronoUnit.MINUTES);
        Instant resolutionDeadline = now.plus(slaPolicy.getResolutionDeadlineMinutes(), ChronoUnit.MINUTES);

        // 3. Create and persist operational Job
        Job job = new Job(
                savedRequest,
                dto.getPriority(),
                JobStatus.PENDING,
                responseDeadline,
                resolutionDeadline,
                SlaStatus.HEALTHY
        );
        Job savedJob = jobRepository.save(job);

        // 4. Attach required parts if requested
        List<JobPart> savedParts = new ArrayList<>();
        if (dto.getRequiredParts() != null && !dto.getRequiredParts().isEmpty()) {
            for (JobPartRequirementDto partReq : dto.getRequiredParts()) {
                InventoryItem item = inventoryItemRepository.findById(partReq.getInventoryItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", partReq.getInventoryItemId()));

                JobPart jobPart = new JobPart(savedJob, item, partReq.getQuantity());
                savedParts.add(jobPartRepository.save(jobPart));
            }
            savedJob.setRequiredParts(savedParts);
        }

        // 5. Immutable audit trail
        String payloadJson = String.format(
                "{\"serviceRequestId\":%d,\"priority\":\"%s\",\"responseDeadline\":\"%s\",\"resolutionDeadline\":\"%s\",\"partsCount\":%d}",
                savedRequest.getId(), dto.getPriority(), responseDeadline, resolutionDeadline, savedParts.size()
        );
        AuditEvent auditEvent = new AuditEvent(
                "JOB",
                savedJob.getId(),
                AuditAction.CREATED,
                actorUsername != null ? actorUsername : "system",
                payloadJson
        );
        auditEventRepository.save(auditEvent);

        return mapToServiceRequestResponse(savedRequest, savedJob);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getServiceRequestById(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceRequest", "id", id));
        Job job = jobRepository.findByServiceRequestId(id).orElse(null);
        return mapToServiceRequestResponse(request, job);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getAllServiceRequests(String status) {
        List<ServiceRequest> requests;
        if (status != null && !status.isBlank()) {
            requests = serviceRequestRepository.findByStatus(status.toUpperCase());
        } else {
            requests = serviceRequestRepository.findAllByOrderByCreatedAtDesc();
        }

        return requests.stream()
                .map(req -> {
                    Job job = jobRepository.findByServiceRequestId(req.getId()).orElse(null);
                    return mapToServiceRequestResponse(req, job);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
        return mapToJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs(JobStatus status, Priority priority, SlaStatus slaStatus) {
        List<Job> jobs = jobRepository.findAll();

        return jobs.stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> priority == null || j.getPriority() == priority)
                .filter(j -> slaStatus == null || j.getSlaStatus() == slaStatus)
                .map(this::mapToJobResponse)
                .toList();
    }

    private ServiceRequestResponse mapToServiceRequestResponse(ServiceRequest request, Job job) {
        Skill skill = request.getRequiredSkill();
        SkillResponse skillResponse = skill != null ?
                new SkillResponse(skill.getId(), skill.getCode(), skill.getName(), skill.getDescription()) : null;

        JobResponse jobResponse = job != null ? mapToJobResponse(job) : null;
        Long jobId = job != null ? job.getId() : null;

        return new ServiceRequestResponse(
                request.getId(),
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getServiceAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                skillResponse,
                request.getEstimatedDurationMinutes(),
                request.getStatus(),
                jobId,
                jobResponse,
                request.getCreatedAt()
        );
    }

    private JobResponse mapToJobResponse(Job job) {
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

        Long assignedTechId = null;
        String assignedTechName = null;
        if (job.getAssignments() != null) {
            for (Assignment a : job.getAssignments()) {
                if (a.getStatus() == com.swiftroute.domain.enums.AssignmentStatus.ACTIVE) {
                    assignedTechId = a.getTechnician().getId();
                    assignedTechName = a.getTechnician().getName();
                    break;
                }
            }
        }

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
                assignedTechId,
                assignedTechName,
                partResponses,
                job.getCreatedAt()
        );
    }
}
