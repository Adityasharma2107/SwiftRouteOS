package com.swiftroute.scheduler;

import com.swiftroute.service.SlaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SlaEscalationWorkerTest {

    @Autowired
    private SlaEscalationWorker worker;

    @Test
    @DisplayName("Worker executes periodic SLA escalation cycle without errors")
    void testWorkerExecutesCycleSuccessfully() {
        assertDoesNotThrow(() -> worker.runSlaEscalationCycle());
    }

    @Test
    @DisplayName("Worker handles runtime exceptions gracefully without rethrowing or terminating")
    void testWorkerHandlesExceptionGracefully() {
        AtomicInteger callCount = new AtomicInteger();
        SlaService throwingService = new SlaService(null, null, null, null) {
            @Override
            public int evaluateAndEscalateActiveJobs() {
                callCount.incrementAndGet();
                throw new RuntimeException("Simulated database failure");
            }
        };

        SlaEscalationWorker resilientWorker = new SlaEscalationWorker(throwingService);
        assertDoesNotThrow(() -> resilientWorker.runSlaEscalationCycle());
        assertEquals(1, callCount.get());
    }
}
