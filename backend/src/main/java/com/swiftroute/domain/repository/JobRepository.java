package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.SlaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
    List<Job> findByStatusIn(Collection<JobStatus> statuses);
    Optional<Job> findByServiceRequestId(Long serviceRequestId);

    /**
     * Find active jobs that have not been completed/cancelled and whose SLA can still be monitored/escalated.
     */
    @Query("""
        SELECT j FROM Job j
        WHERE j.status NOT IN ('COMPLETED', 'CANCELLED')
        AND j.slaStatus <> 'BREACHED'
    """)
    List<Job> findActiveJobsForSlaMonitoring();

    List<Job> findBySlaStatus(SlaStatus slaStatus);
}
