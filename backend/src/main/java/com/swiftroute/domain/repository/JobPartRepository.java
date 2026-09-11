package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.JobPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPartRepository extends JpaRepository<JobPart, Long> {
    List<JobPart> findByJobId(Long jobId);
    Optional<JobPart> findByJobIdAndInventoryItemId(Long jobId, Long inventoryItemId);
}
