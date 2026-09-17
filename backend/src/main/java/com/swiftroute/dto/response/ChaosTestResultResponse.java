package com.swiftroute.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ChaosTestResultResponse {

    private String itemName;
    private String partNumber;
    private int initialStock;
    private int concurrencyLevel;
    private int successfulReservations;
    private int rejectedRequests;
    private int finalQuantityOnHand;
    private int finalQuantityReserved;
    private int finalAvailableStock;
    private boolean zeroOversellGuaranteed;
    private List<String> executionLog = new ArrayList<>();
    private long durationMs;

    public ChaosTestResultResponse() {
    }

    public ChaosTestResultResponse(String itemName, String partNumber, int initialStock, int concurrencyLevel,
                                   int successfulReservations, int rejectedRequests, int finalQuantityOnHand,
                                   int finalQuantityReserved, int finalAvailableStock, boolean zeroOversellGuaranteed,
                                   List<String> executionLog, long durationMs) {
        this.itemName = itemName;
        this.partNumber = partNumber;
        this.initialStock = initialStock;
        this.concurrencyLevel = concurrencyLevel;
        this.successfulReservations = successfulReservations;
        this.rejectedRequests = rejectedRequests;
        this.finalQuantityOnHand = finalQuantityOnHand;
        this.finalQuantityReserved = finalQuantityReserved;
        this.finalAvailableStock = finalAvailableStock;
        this.zeroOversellGuaranteed = zeroOversellGuaranteed;
        this.executionLog = executionLog != null ? executionLog : new ArrayList<>();
        this.durationMs = durationMs;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public int getInitialStock() {
        return initialStock;
    }

    public void setInitialStock(int initialStock) {
        this.initialStock = initialStock;
    }

    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    public void setConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    public int getSuccessfulReservations() {
        return successfulReservations;
    }

    public void setSuccessfulReservations(int successfulReservations) {
        this.successfulReservations = successfulReservations;
    }

    public int getRejectedRequests() {
        return rejectedRequests;
    }

    public void setRejectedRequests(int rejectedRequests) {
        this.rejectedRequests = rejectedRequests;
    }

    public int getFinalQuantityOnHand() {
        return finalQuantityOnHand;
    }

    public void setFinalQuantityOnHand(int finalQuantityOnHand) {
        this.finalQuantityOnHand = finalQuantityOnHand;
    }

    public int getFinalQuantityReserved() {
        return finalQuantityReserved;
    }

    public void setFinalQuantityReserved(int finalQuantityReserved) {
        this.finalQuantityReserved = finalQuantityReserved;
    }

    public int getFinalAvailableStock() {
        return finalAvailableStock;
    }

    public void setFinalAvailableStock(int finalAvailableStock) {
        this.finalAvailableStock = finalAvailableStock;
    }

    public boolean isZeroOversellGuaranteed() {
        return zeroOversellGuaranteed;
    }

    public void setZeroOversellGuaranteed(boolean zeroOversellGuaranteed) {
        this.zeroOversellGuaranteed = zeroOversellGuaranteed;
    }

    public List<String> getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(List<String> executionLog) {
        this.executionLog = executionLog;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
