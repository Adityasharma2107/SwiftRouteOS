package com.swiftroute.dto.response;

public class JobPartResponse {

    private Long id;
    private Long inventoryItemId;
    private String partNumber;
    private String partName;
    private Integer quantityRequired;

    public JobPartResponse() {
    }

    public JobPartResponse(Long id, Long inventoryItemId, String partNumber, String partName, Integer quantityRequired) {
        this.id = id;
        this.inventoryItemId = inventoryItemId;
        this.partNumber = partNumber;
        this.partName = partName;
        this.quantityRequired = quantityRequired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public Integer getQuantityRequired() {
        return quantityRequired;
    }

    public void setQuantityRequired(Integer quantityRequired) {
        this.quantityRequired = quantityRequired;
    }
}
