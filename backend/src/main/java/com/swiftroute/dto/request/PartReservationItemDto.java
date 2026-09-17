package com.swiftroute.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PartReservationItemDto {

    @NotNull(message = "Inventory item ID is required")
    private Long inventoryItemId;

    @NotNull(message = "Quantity to reserve is required")
    @Min(value = 1, message = "Quantity to reserve must be at least 1")
    private Integer quantity;

    public PartReservationItemDto() {
    }

    public PartReservationItemDto(Long inventoryItemId, Integer quantity) {
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
