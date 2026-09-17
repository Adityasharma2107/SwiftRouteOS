package com.swiftroute.service;

import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.entity.InventoryItem;
import com.swiftroute.domain.entity.InventoryReservation;
import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.JobPart;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.ReservationStatus;
import com.swiftroute.domain.repository.AuditEventRepository;
import com.swiftroute.domain.repository.InventoryItemRepository;
import com.swiftroute.domain.repository.InventoryReservationRepository;
import com.swiftroute.domain.repository.JobRepository;
import com.swiftroute.dto.request.PartReservationItemDto;
import com.swiftroute.dto.response.InventoryItemResponse;
import com.swiftroute.dto.response.ReservationResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.exception.InsufficientInventoryException;
import com.swiftroute.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final JobRepository jobRepository;
    private final AuditEventRepository auditEventRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryReservationRepository inventoryReservationRepository,
                            JobRepository jobRepository,
                            AuditEventRepository auditEventRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.jobRepository = jobRepository;
        this.auditEventRepository = auditEventRepository;
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
