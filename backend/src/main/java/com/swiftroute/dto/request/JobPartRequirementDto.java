package com.swiftroute.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class JobPartRequirementDto {

    @NotNull(message = "Inventory item ID is required")
    private Long inventoryItemId;

    @NotNull(message = "Quantity required is required")
    @Min(value = 1, message = "Quantity required must be at least 1")
    private Integer quantity;

    public JobPartRequirementDto() {
    }

    public JobPartRequirementDto(Long inventoryItemId, Integer quantity) {
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
    }

    public Long getInventoryItemId() {
        return inventoryItemId;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
