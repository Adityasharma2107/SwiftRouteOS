package com.swiftroute.exception;

public class InsufficientInventoryException extends RuntimeException {

    private final Long itemId;
    private final String partNumber;
    private final int requested;
    private final int available;

    public InsufficientInventoryException(Long itemId, String partNumber, int requested, int available) {
        super(String.format("Insufficient inventory for part '%s' (ID %d). Requested: %d, Available: %d",
                partNumber, itemId, requested, available));
        this.itemId = itemId;
        this.partNumber = partNumber;
        this.requested = requested;
        this.available = available;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
