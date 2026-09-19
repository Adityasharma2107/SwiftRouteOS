package com.swiftroute.service;

import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.entity.Skill;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.domain.repository.ServiceRequestRepository;
import com.swiftroute.domain.repository.SkillRepository;
import com.swiftroute.dto.request.PartReservationItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InventoryDeadlockPreventionTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SkillRepository skillRepository;

    private Long createTestJob(String name) {
        Skill skill = skillRepository.findAll().stream().findFirst().orElseThrow();
        ServiceRequest sr = new ServiceRequest(
                name,
                "212-555-9900",
                "Deadlock Test Suite Lab",
                40.7580,
                -73.9855,
                "Multi-Part Maintenance",
                "Testing ordered lock acquisition",
                Priority.HIGH,
                skill,
                60,
                "PENDING"
        );
        ServiceRequest savedSr = serviceRequestRepository.save(sr);
        Job job = new Job(
                savedSr,
                Priority.HIGH,
                JobStatus.ASSIGNED,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(14400),
                null
        );
        return jobRepository.save(job).getId();
    }

    @Test
    @DisplayName("Inverted multi-part reservations execute without cyclic deadlocks due to ascending ID sorting")
    void testConcurrentInvertedReservationsDoNotDeadlock() throws InterruptedException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger deadlockCount = new AtomicInteger(0);
        List<Long> jobIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            jobIds.add(createTestJob("Deadlock Job #" + (i + 1)));
        }

        // Two items with IDs 1 and 2 (or 2 and 3)
        long itemA = 1L;
        long itemB = 2L;

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Long jobId = jobIds.get(i);

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronized blast release

                    List<PartReservationItemDto> items = new ArrayList<>();
                    // Alternate order between threads: even threads request [A, B], odd threads request [B, A]
                    if (index % 2 == 0) {
                        items.add(new PartReservationItemDto(itemA, 1));
                        items.add(new PartReservationItemDto(itemB, 1));
                    } else {
                        items.add(new PartReservationItemDto(itemB, 1));
                        items.add(new PartReservationItemDto(itemA, 1));
                    }

                    try {
                        inventoryService.reservePartsForJob(jobId, items, "deadlock-test-worker-" + index);
                        completedCount.incrementAndGet();
                    } catch (Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                        if (msg.contains("deadlock") || msg.contains("40P01")) {
                            deadlockCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Ignore interrupted latch
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads concurrently

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(finished, "All reservation threads should terminate cleanly within timeout");

        // Zero deadlocks must be observed due to ascending lock sorting in reservePartsForJob
        assertEquals(0, deadlockCount.get(), "Zero database deadlocks must occur even with completely inverted request orders");
    }
}
