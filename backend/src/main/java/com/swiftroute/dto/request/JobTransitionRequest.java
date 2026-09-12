package com.swiftroute.dto.request;

import com.swiftroute.domain.enums.JobStatus;
import jakarta.validation.constraints.NotNull;

public class JobTransitionRequest {

    @NotNull(message = "Target status is required (EN_ROUTE, IN_PROGRESS, COMPLETED, CANCELLED)")
    private JobStatus targetStatus;

    private String notes;

    public JobTransitionRequest() {
    }

    public JobTransitionRequest(JobStatus targetStatus, String notes) {
        this.targetStatus = targetStatus;
        this.notes = notes;
    }

    public JobStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(JobStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
