package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.Priority;

import java.time.Instant;

public class ServiceRequestResponse {

    private Long id;
    private String customerName;
    private String customerPhone;
    private String serviceAddress;
    private Double latitude;
    private Double longitude;
    private String title;
    private String description;
    private Priority priority;
    private SkillResponse requiredSkill;
    private Integer estimatedDurationMinutes;
    private String status;
    private Long jobId;
    private JobResponse job;
    private Instant createdAt;

    public ServiceRequestResponse() {
    }

    public ServiceRequestResponse(Long id, String customerName, String customerPhone, String serviceAddress,
                                  Double latitude, Double longitude, String title, String description,
                                  Priority priority, SkillResponse requiredSkill, Integer estimatedDurationMinutes,
                                  String status, Long jobId, JobResponse job, Instant createdAt) {
        this.id = id;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.serviceAddress = serviceAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.requiredSkill = requiredSkill;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.status = status;
        this.jobId = jobId;
        this.job = job;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public SkillResponse getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(SkillResponse requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public JobResponse getJob() {
        return job;
    }

    public void setJob(JobResponse job) {
        this.job = job;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
