package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.TechnicianStatus;

public class TechnicianCandidateResponse {

    private Long technicianId;
    private String technicianName;
    private String phone;
    private TechnicianStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private Integer proficiencyLevel;
    private double distanceKm;
    private long activeJobsToday;
    private int maxDailyJobs;
    private int rank;
    private boolean recommended;
    private ScoreBreakdownResponse scoreBreakdown;

    public TechnicianCandidateResponse() {
    }

    public TechnicianCandidateResponse(Long technicianId, String technicianName, String phone,
                                       TechnicianStatus status, Double currentLatitude, Double currentLongitude,
                                       Integer proficiencyLevel, double distanceKm, long activeJobsToday,
                                       int maxDailyJobs, int rank, boolean recommended,
                                       ScoreBreakdownResponse scoreBreakdown) {
        this.technicianId = technicianId;
        this.technicianName = technicianName;
        this.phone = phone;
        this.status = status;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.proficiencyLevel = proficiencyLevel;
        this.distanceKm = distanceKm;
        this.activeJobsToday = activeJobsToday;
        this.maxDailyJobs = maxDailyJobs;
        this.rank = rank;
        this.recommended = recommended;
        this.scoreBreakdown = scoreBreakdown;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public TechnicianStatus getStatus() {
        return status;
    }

    public void setStatus(TechnicianStatus status) {
        this.status = status;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(Integer proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public long getActiveJobsToday() {
        return activeJobsToday;
    }

    public void setActiveJobsToday(long activeJobsToday) {
        this.activeJobsToday = activeJobsToday;
    }

    public int getMaxDailyJobs() {
        return maxDailyJobs;
    }

    public void setMaxDailyJobs(int maxDailyJobs) {
        this.maxDailyJobs = maxDailyJobs;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public ScoreBreakdownResponse getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(ScoreBreakdownResponse scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }
}
