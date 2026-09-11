package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.Assignment;
import com.swiftroute.domain.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByJobId(Long jobId);
    Optional<Assignment> findByJobIdAndStatus(Long jobId, AssignmentStatus status);
    List<Assignment> findByTechnicianIdAndStatus(Long technicianId, AssignmentStatus status);

    /**
     * Scheduling Overlap Invariant Query:
     * Returns any active assignments for this technician that overlap with [startTime, endTime].
     * Two intervals [S1, E1] and [S2, E2] overlap if: S1 < E2 AND E1 > S2.
     */
    @Query("""
        SELECT a FROM Assignment a
        WHERE a.technician.id = :technicianId
        AND a.status = 'ACTIVE'
        AND a.scheduledStartTime < :endTime
        AND a.scheduledEndTime > :startTime
    """)
    List<Assignment> findOverlappingAssignments(
        @Param("technicianId") Long technicianId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count active assignments for a technician today.
     */
    @Query("""
        SELECT COUNT(a) FROM Assignment a
        WHERE a.technician.id = :technicianId
        AND a.status = 'ACTIVE'
        AND a.scheduledStartTime >= :startOfDay
    """)
    long countTechnicianActiveJobsSince(
        @Param("technicianId") Long technicianId,
        @Param("startOfDay") Instant startOfDay
    );
}
