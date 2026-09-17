package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;

import java.time.Instant;

public class SlaEvaluationResult {

    private Long jobId;
    private Priority priority;
    private SlaStatus currentSlaStatus;
    private double responseElapsedPercent;
    private double resolutionElapsedPercent;
    private long responseRemainingSeconds;
    private long resolutionRemainingSeconds;
    private boolean responseBreached;
    private boolean resolutionBreached;
    private Instant evaluatedAt;

    public SlaEvaluationResult() {
    }

    public SlaEvaluationResult(Long jobId, Priority priority, SlaStatus currentSlaStatus,
                               double responseElapsedPercent, double resolutionElapsedPercent,
                               long responseRemainingSeconds, long resolutionRemainingSeconds,
                               boolean responseBreached, boolean resolutionBreached, Instant evaluatedAt) {
        this.jobId = jobId;
        this.priority = priority;
        this.currentSlaStatus = currentSlaStatus;
        this.responseElapsedPercent = responseElapsedPercent;
        this.resolutionElapsedPercent = resolutionElapsedPercent;
        this.responseRemainingSeconds = responseRemainingSeconds;
        this.resolutionRemainingSeconds = resolutionRemainingSeconds;
        this.responseBreached = responseBreached;
        this.resolutionBreached = resolutionBreached;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public SlaStatus getCurrentSlaStatus() {
        return currentSlaStatus;
    }

    public void setCurrentSlaStatus(SlaStatus currentSlaStatus) {
        this.currentSlaStatus = currentSlaStatus;
    }

    public double getResponseElapsedPercent() {
        return responseElapsedPercent;
    }

    public void setResponseElapsedPercent(double responseElapsedPercent) {
        this.responseElapsedPercent = responseElapsedPercent;
    }

    public double getResolutionElapsedPercent() {
        return resolutionElapsedPercent;
    }

    public void setResolutionElapsedPercent(double resolutionElapsedPercent) {
        this.resolutionElapsedPercent = resolutionElapsedPercent;
    }

    public long getResponseRemainingSeconds() {
        return responseRemainingSeconds;
    }

    public void setResponseRemainingSeconds(long responseRemainingSeconds) {
        this.responseRemainingSeconds = responseRemainingSeconds;
    }

    public long getResolutionRemainingSeconds() {
        return resolutionRemainingSeconds;
    }

    public void setResolutionRemainingSeconds(long resolutionRemainingSeconds) {
        this.resolutionRemainingSeconds = resolutionRemainingSeconds;
    }

    public boolean isResponseBreached() {
        return responseBreached;
    }

    public void setResponseBreached(boolean responseBreached) {
        this.responseBreached = responseBreached;
    }

    public boolean isResolutionBreached() {
        return resolutionBreached;
    }

    public void setResolutionBreached(boolean resolutionBreached) {
        this.resolutionBreached = resolutionBreached;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
