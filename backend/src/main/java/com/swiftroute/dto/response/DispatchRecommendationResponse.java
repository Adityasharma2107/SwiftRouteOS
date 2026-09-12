package com.swiftroute.dto.response;

import java.util.ArrayList;
import java.util.List;

public class DispatchRecommendationResponse {

    private Long jobId;
    private Long serviceRequestId;
    private String requiredSkillCode;
    private String requiredSkillName;
    private Double jobLatitude;
    private Double jobLongitude;
    private int candidatesCount;
    private TechnicianCandidateResponse recommendedCandidate;
    private List<TechnicianCandidateResponse> candidates = new ArrayList<>();

    public DispatchRecommendationResponse() {
    }

    public DispatchRecommendationResponse(Long jobId, Long serviceRequestId, String requiredSkillCode,
                                          String requiredSkillName, Double jobLatitude, Double jobLongitude,
                                          int candidatesCount, TechnicianCandidateResponse recommendedCandidate,
                                          List<TechnicianCandidateResponse> candidates) {
        this.jobId = jobId;
        this.serviceRequestId = serviceRequestId;
        this.requiredSkillCode = requiredSkillCode;
        this.requiredSkillName = requiredSkillName;
        this.jobLatitude = jobLatitude;
        this.jobLongitude = jobLongitude;
        this.candidatesCount = candidatesCount;
        this.recommendedCandidate = recommendedCandidate;
        this.candidates = candidates != null ? candidates : new ArrayList<>();
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(Long serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    public String getRequiredSkillCode() {
        return requiredSkillCode;
    }

    public void setRequiredSkillCode(String requiredSkillCode) {
        this.requiredSkillCode = requiredSkillCode;
    }

    public String getRequiredSkillName() {
        return requiredSkillName;
    }

    public void setRequiredSkillName(String requiredSkillName) {
        this.requiredSkillName = requiredSkillName;
    }

    public Double getJobLatitude() {
        return jobLatitude;
    }

    public void setJobLatitude(Double jobLatitude) {
        this.jobLatitude = jobLatitude;
    }

    public Double getJobLongitude() {
        return jobLongitude;
    }

    public void setJobLongitude(Double jobLongitude) {
        this.jobLongitude = jobLongitude;
    }

    public int getCandidatesCount() {
        return candidatesCount;
    }

    public void setCandidatesCount(int candidatesCount) {
        this.candidatesCount = candidatesCount;
    }

    public TechnicianCandidateResponse getRecommendedCandidate() {
        return recommendedCandidate;
    }

    public void setRecommendedCandidate(TechnicianCandidateResponse recommendedCandidate) {
        this.recommendedCandidate = recommendedCandidate;
    }

    public List<TechnicianCandidateResponse> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<TechnicianCandidateResponse> candidates) {
        this.candidates = candidates;
    }
}
