package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.SlaEscalationEvent;
import com.swiftroute.domain.enums.EscalationStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaEscalationEventRepository extends JpaRepository<SlaEscalationEvent, Long> {
    /**
     * Idempotency check: returns true if an escalation event for this threshold stage has already fired.
     */
    boolean existsByJobIdAndThresholdStage(Long jobId, EscalationStage thresholdStage);

    List<SlaEscalationEvent> findByJobIdOrderByTriggeredAtDesc(Long jobId);
}
