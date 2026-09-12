package com.swiftroute.dto.request;

import com.swiftroute.domain.enums.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CreateServiceRequestDto {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone number is required")
    private String customerPhone;

    @NotBlank(message = "Service location address is required")
    private String serviceAddress;

    @NotNull(message = "Latitude coordinate is required")
    private Double latitude;

    @NotNull(message = "Longitude coordinate is required")
    private Double longitude;

    @NotBlank(message = "Service request title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required (CRITICAL, HIGH, MEDIUM, LOW)")
    private Priority priority;

    @NotNull(message = "Required skill ID is required")
    private Long requiredSkillId;

    @NotNull(message = "Estimated duration in minutes is required")
    @Min(value = 15, message = "Estimated duration must be at least 15 minutes")
    private Integer estimatedDurationMinutes = 60;

    @Valid
    private List<JobPartRequirementDto> requiredParts = new ArrayList<>();

    public CreateServiceRequestDto() {
    }

    public CreateServiceRequestDto(String customerName, String customerPhone, String serviceAddress,
                                   Double latitude, Double longitude, String title, String description,
                                   Priority priority, Long requiredSkillId, Integer estimatedDurationMinutes,
                                   List<JobPartRequirementDto> requiredParts) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.serviceAddress = serviceAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.requiredSkillId = requiredSkillId;
        this.estimatedDurationMinutes = estimatedDurationMinutes != null ? estimatedDurationMinutes : 60;
        this.requiredParts = requiredParts != null ? requiredParts : new ArrayList<>();
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

    public Long getRequiredSkillId() {
        return requiredSkillId;
    }

    public void setRequiredSkillId(Long requiredSkillId) {
        this.requiredSkillId = requiredSkillId;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public List<JobPartRequirementDto> getRequiredParts() {
        return requiredParts;
    }

    public void setRequiredParts(List<JobPartRequirementDto> requiredParts) {
        this.requiredParts = requiredParts;
    }
}
