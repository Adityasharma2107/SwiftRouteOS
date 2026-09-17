package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.EscalationStage;

import java.time.Instant;

public class SlaEscalationEventResponse {

    private Long id;
    private Long jobId;
    private EscalationStage thresholdStage;
    private Instant triggeredAt;
    private String details;

    public SlaEscalationEventResponse() {
    }

    public SlaEscalationEventResponse(Long id, Long jobId, EscalationStage thresholdStage, Instant triggeredAt, String details) {
        this.id = id;
        this.jobId = jobId;
        this.thresholdStage = thresholdStage;
        this.triggeredAt = triggeredAt;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public EscalationStage getThresholdStage() {
        return thresholdStage;
    }

    public void setThresholdStage(EscalationStage thresholdStage) {
        this.thresholdStage = thresholdStage;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
