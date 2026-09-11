package com.swiftroute.domain.entity;

import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false, unique = true)
    private ServiceRequest serviceRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "sla_response_deadline", nullable = false)
    private Instant slaResponseDeadline;

    @Column(name = "sla_resolution_deadline", nullable = false)
    private Instant slaResolutionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", nullable = false, length = 30)
    private SlaStatus slaStatus = SlaStatus.HEALTHY;

    @Column(name = "actual_response_at")
    private Instant actualResponseAt;

    @Column(name = "actual_resolution_at")
    private Instant actualResolutionAt;

    @Column(name = "scheduled_start")
    private Instant scheduledStart;

    @Column(name = "scheduled_end")
    private Instant scheduledEnd;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobPart> requiredParts = new ArrayList<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private List<Assignment> assignments = new ArrayList<>();

    public Job() {
    }

    public Job(ServiceRequest serviceRequest, Priority priority, JobStatus status, Instant slaResponseDeadline, Instant slaResolutionDeadline, SlaStatus slaStatus) {
        this.serviceRequest = serviceRequest;
        this.priority = priority;
        this.status = status != null ? status : JobStatus.PENDING;
        this.slaResponseDeadline = slaResponseDeadline;
        this.slaResolutionDeadline = slaResolutionDeadline;
        this.slaStatus = slaStatus != null ? slaStatus : SlaStatus.HEALTHY;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
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

    public List<JobPart> getRequiredParts() {
        return requiredParts;
    }

    public void setRequiredParts(List<JobPart> requiredParts) {
        this.requiredParts = requiredParts;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }
}
