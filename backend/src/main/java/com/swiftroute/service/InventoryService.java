package com.swiftroute.service;

import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.ReservationStatus;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.request.PartReservationItemDto;
import com.swiftroute.dto.response.ChaosTestResultResponse;
import com.swiftroute.dto.response.InventoryItemResponse;
import com.swiftroute.dto.response.ReservationResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.exception.InsufficientInventoryException;
import com.swiftroute.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final JobRepository jobRepository;
    private final AuditEventRepository auditEventRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final SkillRepository skillRepository;
    private final PlatformTransactionManager transactionManager;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryReservationRepository inventoryReservationRepository,
                            JobRepository jobRepository,
                            AuditEventRepository auditEventRepository,
                            ServiceRequestRepository serviceRequestRepository,
                            SkillRepository skillRepository,
                            PlatformTransactionManager transactionManager) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.jobRepository = jobRepository;
        this.auditEventRepository = auditEventRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.skillRepository = skillRepository;
        this.transactionManager = transactionManager;
    }

    /**
     * Concurrency-safe parts reservation using database-level pessimistic write locking (SELECT ... FOR UPDATE).
     * Items are sorted in ascending order by inventoryItemId before lock acquisition to strictly prevent deadlocks.
     */
    @Transactional
    public List<ReservationResponse> reservePartsForJob(Long jobId, List<PartReservationItemDto> items, String actorUsername) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot reserve inventory for a job that is " + job.getStatus());
        }

        // Deadlock prevention: sort items ascending by ID
        List<PartReservationItemDto> sortedItems = items.stream()
                .sorted(Comparator.comparingLong(PartReservationItemDto::getInventoryItemId))
                .toList();

        List<ReservationResponse> responses = new ArrayList<>();

        for (PartReservationItemDto reqItem : sortedItems) {
            // Acquire row-level pessimistic write lock
            InventoryItem inventoryItem = inventoryItemRepository.findByIdWithPessimisticLock(reqItem.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", reqItem.getInventoryItemId()));

            int onHand = inventoryItem.getQuantityOnHand() != null ? inventoryItem.getQuantityOnHand() : 0;
            int reserved = inventoryItem.getQuantityReserved() != null ? inventoryItem.getQuantityReserved() : 0;
            int available = onHand - reserved;

            if (available < reqItem.getQuantity()) {
                throw new InsufficientInventoryException(
                        inventoryItem.getId(),
                        inventoryItem.getPartNumber(),
                        reqItem.getQuantity(),
                        available
                );
            }

            // Update reserved count on inventory item
            int newReserved = reserved + reqItem.getQuantity();
            inventoryItem.setQuantityReserved(newReserved);
            inventoryItemRepository.save(inventoryItem);

            // Record reservation entity
            InventoryReservation reservation = new InventoryReservation(
                    job,
                    inventoryItem,
                    reqItem.getQuantity(),
                    ReservationStatus.RESERVED
            );
            InventoryReservation savedReservation = inventoryReservationRepository.save(reservation);

            // Immutable audit event
            String payload = String.format(
                    "{\"jobId\":%d,\"itemId\":%d,\"partNumber\":\"%s\",\"quantityReserved\":%d,\"newReservedTotal\":%d,\"availableRemaining\":%d}",
                    jobId,
                    inventoryItem.getId(),
                    inventoryItem.getPartNumber(),
                    reqItem.getQuantity(),
                    newReserved,
                    onHand - newReserved
            );
            AuditEvent auditEvent = new AuditEvent(
                    "INVENTORY",
                    inventoryItem.getId(),
                    AuditAction.INVENTORY_RESERVED,
                    actorUsername != null ? actorUsername : "system",
                    payload
            );
            auditEventRepository.save(auditEvent);

            responses.add(mapToReservationResponse(savedReservation));
        }

        return responses;
    }

    /**
     * Auto-reserves required parts that were attached to the job upon service request creation.
     */
    @Transactional
    public List<ReservationResponse> reserveRequiredPartsForJob(Long jobId, String actorUsername) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", jobId));

        List<JobPart> requiredParts = job.getRequiredParts();
        if (requiredParts == null || requiredParts.isEmpty()) {
            return List.of();
        }

        List<PartReservationItemDto> items = requiredParts.stream()
                .map(part -> new PartReservationItemDto(part.getInventoryItem().getId(), part.getQuantityRequired()))
                .toList();

        return reservePartsForJob(jobId, items, actorUsername);
    }

    /**
     * Releases active reservations for a job (e.g. upon job cancellation or dispatcher manual rollback).
     * Restores quantity_reserved back to available stock.
     */
    @Transactional
    public List<ReservationResponse> releaseReservationsForJob(Long jobId, String reason, String actorUsername) {
        List<InventoryReservation> activeReservations = inventoryReservationRepository.findByJobIdAndStatus(jobId, ReservationStatus.RESERVED);
        if (activeReservations.isEmpty()) {
            return List.of();
        }

        // Deadlock prevention
        List<InventoryReservation> sortedReservations = activeReservations.stream()
                .sorted(Comparator.comparingLong(r -> r.getInventoryItem().getId()))
                .toList();

        List<ReservationResponse> responses = new ArrayList<>();
        Instant now = Instant.now();

        for (InventoryReservation res : sortedReservations) {
            InventoryItem inventoryItem = inventoryItemRepository.findByIdWithPessimisticLock(res.getInventoryItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", res.getInventoryItem().getId()));

            int currentReserved = inventoryItem.getQuantityReserved() != null ? inventoryItem.getQuantityReserved() : 0;
            int restoredReserved = Math.max(0, currentReserved - res.getQuantityReserved());
            inventoryItem.setQuantityReserved(restoredReserved);
            inventoryItemRepository.save(inventoryItem);

            res.setStatus(ReservationStatus.RELEASED);
            res.setReleasedAt(now);
            InventoryReservation savedRes = inventoryReservationRepository.save(res);

            String payload = String.format(
                    "{\"jobId\":%d,\"itemId\":%d,\"partNumber\":\"%s\",\"quantityReleased\":%d,\"reason\":\"%s\"}",
                    jobId,
                    inventoryItem.getId(),
                    inventoryItem.getPartNumber(),
                    res.getQuantityReserved(),
                    reason != null ? reason.replace("\"", "\\\"") : "Job cancelled or unassigned"
            );
            AuditEvent auditEvent = new AuditEvent(
                    "INVENTORY",
                    inventoryItem.getId(),
                    AuditAction.INVENTORY_RELEASED,
                    actorUsername != null ? actorUsername : "system",
                    payload
            );
            auditEventRepository.save(auditEvent);

            responses.add(mapToReservationResponse(savedRes));
        }

        return responses;
    }

    /**
     * Consumes active reservations upon successful job completion.
     * Permanently deducts quantity from quantity_on_hand and frees quantity_reserved.
     */
    @Transactional
    public List<ReservationResponse> consumeReservationsForJob(Long jobId, String actorUsername) {
        List<InventoryReservation> activeReservations = inventoryReservationRepository.findByJobIdAndStatus(jobId, ReservationStatus.RESERVED);
        if (activeReservations.isEmpty()) {
            return List.of();
        }

        List<InventoryReservation> sortedReservations = activeReservations.stream()
                .sorted(Comparator.comparingLong(r -> r.getInventoryItem().getId()))
                .toList();

        List<ReservationResponse> responses = new ArrayList<>();

        for (InventoryReservation res : sortedReservations) {
            InventoryItem inventoryItem = inventoryItemRepository.findByIdWithPessimisticLock(res.getInventoryItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", res.getInventoryItem().getId()));

            int onHand = inventoryItem.getQuantityOnHand() != null ? inventoryItem.getQuantityOnHand() : 0;
            int reserved = inventoryItem.getQuantityReserved() != null ? inventoryItem.getQuantityReserved() : 0;

            int newOnHand = Math.max(0, onHand - res.getQuantityReserved());
            int newReserved = Math.max(0, reserved - res.getQuantityReserved());

            inventoryItem.setQuantityOnHand(newOnHand);
            inventoryItem.setQuantityReserved(newReserved);
            inventoryItemRepository.save(inventoryItem);

            res.setStatus(ReservationStatus.CONSUMED);
            InventoryReservation savedRes = inventoryReservationRepository.save(res);

            String payload = String.format(
                    "{\"jobId\":%d,\"itemId\":%d,\"partNumber\":\"%s\",\"quantityConsumed\":%d,\"remainingOnHand\":%d}",
                    jobId,
                    inventoryItem.getId(),
                    inventoryItem.getPartNumber(),
                    res.getQuantityReserved(),
                    newOnHand
            );
            AuditEvent auditEvent = new AuditEvent(
                    "INVENTORY",
                    inventoryItem.getId(),
                    AuditAction.INVENTORY_CONSUMED,
                    actorUsername != null ? actorUsername : "system",
                    payload
            );
            auditEventRepository.save(auditEvent);

            responses.add(mapToReservationResponse(savedRes));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsForJob(Long jobId) {
        return inventoryReservationRepository.findByJobId(jobId).stream()
                .map(this::mapToReservationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(this::mapToInventoryItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getInventoryItemById(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", id));
        return mapToInventoryItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getLowStockItems() {
        return inventoryItemRepository.findLowStockItems().stream()
                .map(this::mapToInventoryItemResponse)
                .toList();
    }

    /**
     * Chaos concurrency test harness:
     * Dispatches multiple concurrent threads simultaneously attempting to reserve 1 unit of a scarce inventory item.
     * Database-level row locks (SELECT ... FOR UPDATE) guarantee that at most 1 reservation succeeds and zero overselling occurs.
     */
    public ChaosTestResultResponse runChaosSimulation(int threadsCount, boolean resetAfterTest) {
        long startTime = System.currentTimeMillis();
        int concurrency = Math.max(2, Math.min(threadsCount, 20));

        InventoryItem item = inventoryItemRepository.findByPartNumber("SCARCE-SENSOR-CHILLER")
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "partNumber", "SCARCE-SENSOR-CHILLER"));

        int initialOnHand = item.getQuantityOnHand();
        int initialReserved = item.getQuantityReserved();
        int initialAvailable = initialOnHand - initialReserved;

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Pre-create jobs for each thread so they can run concurrently
        List<Long> jobIds = new ArrayList<>();
        Skill fallbackSkill = skillRepository.findAll().stream().findFirst().orElse(null);

        txTemplate.execute(status -> {
            for (int i = 0; i < concurrency; i++) {
                ServiceRequest sr = new ServiceRequest(
                        "Chaos Customer " + (i + 1),
                        "212-555-0" + String.format("%03d", i),
                        "Chaos Test Lab, Manhattan",
                        40.7580,
                        -73.9855,
                        "Concurrent Stress Job #" + (i + 1),
                        "Concurrency race condition test harness",
                        Priority.HIGH,
                        fallbackSkill,
                        60,
                        "PENDING"
                );
                ServiceRequest savedSr = serviceRequestRepository.save(sr);
                Job job = new Job(
                        savedSr,
                        Priority.HIGH,
                        JobStatus.PENDING,
                        Instant.now().plusSeconds(3600),
                        Instant.now().plusSeconds(14400),
                        null
                );
                Job savedJob = jobRepository.save(job);
                jobIds.add(savedJob.getId());
            }
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<String> logs = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < concurrency; i++) {
            final int threadIndex = i + 1;
            final Long jobId = jobIds.get(i);

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for simultaneous release trigger
                    txTemplate.execute(status -> {
                        // Pessimistic lock reservation inside isolated transaction
                        InventoryItem lockedItem = inventoryItemRepository.findByIdWithPessimisticLock(item.getId())
                                .orElseThrow();

                        int onHand = lockedItem.getQuantityOnHand();
                        int reserved = lockedItem.getQuantityReserved();
                        int available = onHand - reserved;

                        if (available < 1) {
                            throw new InsufficientInventoryException(lockedItem.getId(), lockedItem.getPartNumber(), 1, available);
                        }

                        lockedItem.setQuantityReserved(reserved + 1);
                        inventoryItemRepository.save(lockedItem);

                        Job j = jobRepository.findById(jobId).orElseThrow();
                        InventoryReservation res = new InventoryReservation(j, lockedItem, 1, ReservationStatus.RESERVED);
                        inventoryReservationRepository.save(res);

                        AuditEvent auditEvent = new AuditEvent(
                                "INVENTORY",
                                lockedItem.getId(),
                                AuditAction.INVENTORY_RESERVED,
                                "chaos-thread-" + threadIndex,
                                String.format("{\"thread\":%d,\"jobId\":%d,\"partNumber\":\"%s\",\"quantity\":1,\"newReserved\":%d}",
                                        threadIndex, jobId, lockedItem.getPartNumber(), reserved + 1)
                        );
                        auditEventRepository.save(auditEvent);

                        return null;
                    });

                    successCount.incrementAndGet();
                    logs.add(String.format("[Thread-%02d] SUCCESS: Acquired row lock FOR UPDATE -> Reserved 1 unit for Job #%d", threadIndex, jobId));
                } catch (InsufficientInventoryException e) {
                    rejectedCount.incrementAndGet();
                    logs.add(String.format("[Thread-%02d] REJECTED: Stock exhausted (available=0) -> Transaction rolled back", threadIndex));
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                    logs.add(String.format("[Thread-%02d] FAILED: %s", threadIndex, e.getMessage()));
                }
            });
        }

        try {
            readyLatch.await(5, TimeUnit.SECONDS);
            startLatch.countDown(); // FIRE ALL THREADS SIMULTANEOUSLY
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify state
        InventoryItem finalItem = inventoryItemRepository.findById(item.getId()).orElseThrow();
        int finalOnHand = finalItem.getQuantityOnHand();
        int finalReserved = finalItem.getQuantityReserved();
        int finalAvailable = finalOnHand - finalReserved;

        boolean zeroOversell = finalReserved <= finalOnHand && finalAvailable >= 0;

        // Reset if requested (for repeated demo runs in the Chaos Panel)
        if (resetAfterTest) {
            txTemplate.execute(status -> {
                for (Long jId : jobIds) {
                    List<InventoryReservation> active = inventoryReservationRepository.findByJobIdAndStatus(jId, ReservationStatus.RESERVED);
                    for (InventoryReservation r : active) {
                        r.setStatus(ReservationStatus.RELEASED);
                        r.setReleasedAt(Instant.now());
                        inventoryReservationRepository.saveAndFlush(r);
                    }
                }
                InventoryItem resetItem = inventoryItemRepository.findByIdWithPessimisticLock(item.getId()).orElseThrow();
                resetItem.setQuantityReserved(0);
                inventoryItemRepository.saveAndFlush(resetItem);
                return null;
            });
        }

        long duration = System.currentTimeMillis() - startTime;

        List<String> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort(Comparator.naturalOrder());

        return new ChaosTestResultResponse(
                item.getName(),
                item.getPartNumber(),
                initialAvailable,
                concurrency,
                successCount.get(),
                rejectedCount.get(),
                finalOnHand,
                finalReserved,
                finalAvailable,
                zeroOversell,
                sortedLogs,
                duration
        );
    }


    private ReservationResponse mapToReservationResponse(InventoryReservation res) {
        InventoryItem item = res.getInventoryItem();
        return new ReservationResponse(
                res.getId(),
                res.getJob().getId(),
                item.getId(),
                item.getPartNumber(),
                item.getName(),
                res.getQuantityReserved(),
                res.getStatus(),
                res.getReservedAt(),
                res.getReleasedAt()
        );
    }

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        int onHand = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
        int reserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
        int available = onHand - reserved;
        int threshold = item.getReorderThreshold() != null ? item.getReorderThreshold() : 0;
        boolean lowStock = available <= threshold;

        return new InventoryItemResponse(
                item.getId(),
                item.getPartNumber(),
                item.getName(),
                item.getCategory(),
                onHand,
                reserved,
                available,
                threshold,
                item.getUnitCost(),
                lowStock
        );
    }
}
