package com.swiftroute.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReserveInventoryRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotEmpty(message = "At least one item must be specified for reservation")
    @Valid
    private List<PartReservationItemDto> items = new ArrayList<>();

    private String notes;

    public ReserveInventoryRequest() {
    }

    public ReserveInventoryRequest(Long jobId, List<PartReservationItemDto> items, String notes) {
        this.jobId = jobId;
        this.items = items != null ? items : new ArrayList<>();
        this.notes = notes;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public List<PartReservationItemDto> getItems() {
        return items;
    }

    public void setItems(List<PartReservationItemDto> items) {
        this.items = items;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
