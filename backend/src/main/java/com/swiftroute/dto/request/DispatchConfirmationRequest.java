package com.swiftroute.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class DispatchConfirmationRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotNull(message = "Selected technician ID is required")
    private Long selectedTechnicianId;

    @NotNull(message = "Scheduled start time is required")
    private Instant scheduledStartTime;

    @NotNull(message = "Scheduled end time is required")
    private Instant scheduledEndTime;

    private String overrideReason;

    private String notes;

    public DispatchConfirmationRequest() {
    }

    public DispatchConfirmationRequest(Long jobId, Long selectedTechnicianId, Instant scheduledStartTime, Instant scheduledEndTime, String overrideReason, String notes) {
        this.jobId = jobId;
        this.selectedTechnicianId = selectedTechnicianId;
        this.scheduledStartTime = scheduledStartTime;
        this.scheduledEndTime = scheduledEndTime;
        this.overrideReason = overrideReason;
        this.notes = notes;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getSelectedTechnicianId() {
        return selectedTechnicianId;
    }

    public void setSelectedTechnicianId(Long selectedTechnicianId) {
        this.selectedTechnicianId = selectedTechnicianId;
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

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
