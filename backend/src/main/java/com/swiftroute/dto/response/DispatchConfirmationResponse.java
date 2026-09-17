package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.JobStatus;

import java.time.Instant;

public class DispatchConfirmationResponse {

    private Long assignmentId;
    private Long jobId;
    private Long technicianId;
    private String technicianName;
    private JobStatus jobStatus;
    private Instant scheduledStartTime;
    private Instant scheduledEndTime;
    private boolean manualOverride;
    private String overrideReason;
    private String assignedBy;
    private Instant assignedAt;

    public DispatchConfirmationResponse() {
    }

    public DispatchConfirmationResponse(Long assignmentId, Long jobId, Long technicianId, String technicianName,
                                        JobStatus jobStatus, Instant scheduledStartTime, Instant scheduledEndTime,
                                        boolean manualOverride, String overrideReason, String assignedBy, Instant assignedAt) {
        this.assignmentId = assignmentId;
        this.jobId = jobId;
        this.technicianId = technicianId;
        this.technicianName = technicianName;
        this.jobStatus = jobStatus;
        this.scheduledStartTime = scheduledStartTime;
        this.scheduledEndTime = scheduledEndTime;
        this.manualOverride = manualOverride;
        this.overrideReason = overrideReason;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public Instant getScheduledStartTime() {
        return scheduledStartTime;
    }

    public void setScheduledStartTime(Instant scheduledStartTime) {
        this.scheduledStartTime = scheduledStartTime;
    }

    public Instant getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(Instant scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }
}
