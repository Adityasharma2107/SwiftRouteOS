package com.swiftroute.dto.response;

import java.math.BigDecimal;

public class InventoryItemResponse {

    private Long id;
    private String partNumber;
    private String name;
    private String category;
    private Integer quantityOnHand;
    private Integer quantityReserved;
    private Integer availableQuantity;
    private Integer reorderThreshold;
    private BigDecimal unitCost;
    private boolean lowStock;

    public InventoryItemResponse() {
    }

    public InventoryItemResponse(Long id, String partNumber, String name, String category,
                                 Integer quantityOnHand, Integer quantityReserved, Integer availableQuantity,
                                 Integer reorderThreshold, BigDecimal unitCost, boolean lowStock) {
        this.id = id;
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.availableQuantity = availableQuantity;
        this.reorderThreshold = reorderThreshold;
        this.unitCost = unitCost;
        this.lowStock = lowStock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReorderThreshold() {
        return reorderThreshold;
    }

    public void setReorderThreshold(Integer reorderThreshold) {
        this.reorderThreshold = reorderThreshold;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public void setLowStock(boolean lowStock) {
        this.lowStock = lowStock;
    }
}
