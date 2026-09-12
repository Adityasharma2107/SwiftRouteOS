package com.swiftroute.dto.response;

public class ScoreBreakdownResponse {

    private double skillScore;
    private double distanceScore;
    private double workloadScore;
    private double totalScore;
    private String explanation;

    public ScoreBreakdownResponse() {
    }

    public ScoreBreakdownResponse(double skillScore, double distanceScore, double workloadScore, double totalScore, String explanation) {
        this.skillScore = skillScore;
        this.distanceScore = distanceScore;
        this.workloadScore = workloadScore;
        this.totalScore = totalScore;
        this.explanation = explanation;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(double skillScore) {
        this.skillScore = skillScore;
    }

    public double getDistanceScore() {
        return distanceScore;
    }

    public void setDistanceScore(double distanceScore) {
        this.distanceScore = distanceScore;
    }

    public double getWorkloadScore() {
        return workloadScore;
    }

    public void setWorkloadScore(double workloadScore) {
        this.workloadScore = workloadScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
