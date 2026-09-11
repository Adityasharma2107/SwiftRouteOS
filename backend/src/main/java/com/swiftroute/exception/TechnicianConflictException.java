package com.swiftroute.exception;

import java.time.Instant;

public class TechnicianConflictException extends RuntimeException {

    private final Long technicianId;
    private final Instant conflictStart;
    private final Instant conflictEnd;

    public TechnicianConflictException(Long technicianId, String message) {
        super(message);
        this.technicianId = technicianId;
        this.conflictStart = null;
        this.conflictEnd = null;
    }

    public TechnicianConflictException(Long technicianId, Instant conflictStart, Instant conflictEnd) {
        super(String.format("Technician ID %d has an overlapping active assignment between %s and %s",
                technicianId, conflictStart, conflictEnd));
        this.technicianId = technicianId;
        this.conflictStart = conflictStart;
        this.conflictEnd = conflictEnd;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public Instant getConflictStart() {
        return conflictStart;
    }

    public Instant getConflictEnd() {
        return conflictEnd;
    }
}
