package com.swiftroute.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inventory_items")
public class InventoryItem extends BaseEntity {

    @Column(name = "part_number", nullable = false, unique = true, length = 50)
    private String partNumber;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold = 5;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public InventoryItem() {
    }

    public InventoryItem(String partNumber, String name, String category, Integer quantityOnHand, Integer quantityReserved, Integer reorderThreshold, BigDecimal unitCost, Long version) {
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : 0;
        this.quantityReserved = quantityReserved != null ? quantityReserved : 0;
        this.reorderThreshold = reorderThreshold != null ? reorderThreshold : 5;
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.version = version != null ? version : 0L;
    }

    public Integer getAvailableQuantity() {
        return (quantityOnHand != null ? quantityOnHand : 0) - (quantityReserved != null ? quantityReserved : 0);
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
