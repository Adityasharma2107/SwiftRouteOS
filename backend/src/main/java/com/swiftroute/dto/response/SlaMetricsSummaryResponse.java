package com.swiftroute.dto.response;

public class SlaMetricsSummaryResponse {

    private long totalMonitoredJobs;
    private long healthyJobs;
    private long nearingBreachJobs;
    private long breachedJobs;
    private double complianceRatePercent;

    public SlaMetricsSummaryResponse() {
    }

    public SlaMetricsSummaryResponse(long totalMonitoredJobs, long healthyJobs, long nearingBreachJobs, long breachedJobs, double complianceRatePercent) {
        this.totalMonitoredJobs = totalMonitoredJobs;
        this.healthyJobs = healthyJobs;
        this.nearingBreachJobs = nearingBreachJobs;
        this.breachedJobs = breachedJobs;
        this.complianceRatePercent = complianceRatePercent;
    }

    public long getTotalMonitoredJobs() {
        return totalMonitoredJobs;
    }

    public void setTotalMonitoredJobs(long totalMonitoredJobs) {
        this.totalMonitoredJobs = totalMonitoredJobs;
    }

    public long getHealthyJobs() {
        return healthyJobs;
    }

    public void setHealthyJobs(long healthyJobs) {
        this.healthyJobs = healthyJobs;
    }

    public long getNearingBreachJobs() {
        return nearingBreachJobs;
    }

    public void setNearingBreachJobs(long nearingBreachJobs) {
        this.nearingBreachJobs = nearingBreachJobs;
    }

    public long getBreachedJobs() {
        return breachedJobs;
    }

    public void setBreachedJobs(long breachedJobs) {
        this.breachedJobs = breachedJobs;
    }

    public double getComplianceRatePercent() {
        return complianceRatePercent;
    }

    public void setComplianceRatePercent(double complianceRatePercent) {
        this.complianceRatePercent = complianceRatePercent;
    }
}
