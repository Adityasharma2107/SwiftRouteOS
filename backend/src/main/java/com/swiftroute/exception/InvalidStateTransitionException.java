package com.swiftroute.exception;

public class InvalidStateTransitionException extends RuntimeException {

    private final String fromStatus;
    private final String toStatus;

    public InvalidStateTransitionException(String entityName, String fromStatus, String toStatus) {
        super(String.format("Illegal state transition for %s from '%s' to '%s'", entityName, fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }
}
