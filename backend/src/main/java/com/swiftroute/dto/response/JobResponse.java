package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JobResponse {

    private Long id;
    private Long serviceRequestId;
    private Priority priority;
    private JobStatus status;
    private Instant slaResponseDeadline;
    private Instant slaResolutionDeadline;
    private SlaStatus slaStatus;
    private Instant actualResponseAt;
    private Instant actualResolutionAt;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private List<JobPartResponse> requiredParts = new ArrayList<>();
    private Instant createdAt;

    public JobResponse() {
    }

    public JobResponse(Long id, Long serviceRequestId, Priority priority, JobStatus status,
                       Instant slaResponseDeadline, Instant slaResolutionDeadline, SlaStatus slaStatus,
                       Instant actualResponseAt, Instant actualResolutionAt,
                       Instant scheduledStart, Instant scheduledEnd,
                       Long assignedTechnicianId, String assignedTechnicianName,
                       List<JobPartResponse> requiredParts, Instant createdAt) {
        this.id = id;
        this.serviceRequestId = serviceRequestId;
        this.priority = priority;
        this.status = status;
        this.slaResponseDeadline = slaResponseDeadline;
        this.slaResolutionDeadline = slaResolutionDeadline;
        this.slaStatus = slaStatus;
        this.actualResponseAt = actualResponseAt;
        this.actualResolutionAt = actualResolutionAt;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.assignedTechnicianId = assignedTechnicianId;
        this.assignedTechnicianName = assignedTechnicianName;
        this.requiredParts = requiredParts != null ? requiredParts : new ArrayList<>();
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getSlaResponseDeadline() {
        return slaResponseDeadline;
    }

    public void setSlaResponseDeadline(Instant slaResponseDeadline) {
        this.slaResponseDeadline = slaResponseDeadline;
    }

    public Instant getSlaResolutionDeadline() {
        return slaResolutionDeadline;
    }

    public void setSlaResolutionDeadline(Instant slaResolutionDeadline) {
        this.slaResolutionDeadline = slaResolutionDeadline;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }

    public Instant getActualResponseAt() {
        return actualResponseAt;
    }

    public void setActualResponseAt(Instant actualResponseAt) {
        this.actualResponseAt = actualResponseAt;
    }

    public Instant getActualResolutionAt() {
        return actualResolutionAt;
    }

    public void setActualResolutionAt(Instant actualResolutionAt) {
        this.actualResolutionAt = actualResolutionAt;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(Instant scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public Instant getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(Instant scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public Long getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Long assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedTechnicianName() {
        return assignedTechnicianName;
    }

    public void setAssignedTechnicianName(String assignedTechnicianName) {
        this.assignedTechnicianName = assignedTechnicianName;
    }

    public List<JobPartResponse> getRequiredParts() {
        return requiredParts;
    }

    public void setRequiredParts(List<JobPartResponse> requiredParts) {
        this.requiredParts = requiredParts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
