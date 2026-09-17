package com.swiftroute.dto.response;

import com.swiftroute.domain.enums.ReservationStatus;

import java.time.Instant;

public class ReservationResponse {

    private Long reservationId;
    private Long jobId;
    private Long inventoryItemId;
    private String partNumber;
    private String itemName;
    private Integer quantityReserved;
    private ReservationStatus status;
    private Instant reservedAt;
    private Instant releasedAt;

    public ReservationResponse() {
    }

    public ReservationResponse(Long reservationId, Long jobId, Long inventoryItemId, String partNumber,
                               String itemName, Integer quantityReserved, ReservationStatus status,
                               Instant reservedAt, Instant releasedAt) {
        this.reservationId = reservationId;
        this.jobId = jobId;
        this.inventoryItemId = inventoryItemId;
        this.partNumber = partNumber;
        this.itemName = itemName;
        this.quantityReserved = quantityReserved;
        this.status = status;
        this.reservedAt = reservedAt;
        this.releasedAt = releasedAt;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }
}
