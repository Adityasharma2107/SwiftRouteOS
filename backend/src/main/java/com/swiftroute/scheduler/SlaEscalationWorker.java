package com.swiftroute.scheduler;

import com.swiftroute.service.SlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "swiftroute.sla.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SlaEscalationWorker {

    private static final Logger log = LoggerFactory.getLogger(SlaEscalationWorker.class);

    private final SlaService slaService;

    public SlaEscalationWorker(SlaService slaService) {
        this.slaService = slaService;
    }

    /**
     * Periodically sweeps active jobs, recalculates SLA deadlines, and records
     * idempotent escalation warnings/breaches.
     */
    @Scheduled(fixedRateString = "${swiftroute.sla.scheduler-rate-ms:30000}")
    public void runSlaEscalationCycle() {
        log.debug("Starting scheduled SLA escalation evaluation cycle...");
        try {
            long startTime = System.currentTimeMillis();
            int evaluatedJobsCount = slaService.evaluateAndEscalateActiveJobs();
            long elapsed = System.currentTimeMillis() - startTime;
            if (evaluatedJobsCount > 0) {
                log.info("SLA escalation evaluation cycle completed: evaluated {} active jobs in {} ms",
                        evaluatedJobsCount, elapsed);
            } else {
                log.debug("SLA escalation evaluation cycle completed: no active jobs to evaluate (took {} ms)", elapsed);
            }
        } catch (Exception ex) {
            log.error("Unhandled error during scheduled SLA escalation cycle: {}", ex.getMessage(), ex);
        }
    }
}
