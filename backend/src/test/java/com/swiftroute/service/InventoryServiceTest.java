package com.swiftroute.service;

import com.swiftroute.domain.entity.AuditEvent;
import com.swiftroute.domain.entity.InventoryItem;
import com.swiftroute.domain.entity.Job;
import com.swiftroute.domain.entity.ServiceRequest;
import com.swiftroute.domain.enums.AuditAction;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.ReservationStatus;
import com.swiftroute.domain.repository.*;
import com.swiftroute.dto.request.JobTransitionRequest;
import com.swiftroute.dto.request.PartReservationItemDto;
import com.swiftroute.dto.response.ReservationResponse;
import com.swiftroute.exception.BusinessRuleException;
import com.swiftroute.exception.InsufficientInventoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JobService jobService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Job testJob;
    private InventoryItem capacitorItem;
    private InventoryItem scarceItem;

    @BeforeEach
    void setUp() {
        var skill = skillRepository.findAll().get(0);

        ServiceRequest sr = new ServiceRequest(
                "Inventory Hospital",
                "212-555-0100",
                "100 Medical Center Dr",
                40.7500,
                -73.9800,
                "Chiller maintenance with parts",
                "Filter and capacitor replacement",
                Priority.HIGH,
                skill,
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
        this.testJob = jobRepository.save(job);

        this.capacitorItem = inventoryItemRepository.findByPartNumber("CAP-45UF-440V")
                .orElseThrow(() -> new IllegalStateException("Seed item CAP-45UF-440V missing"));

        this.scarceItem = inventoryItemRepository.findByPartNumber("SCARCE-SENSOR-CHILLER")
                .orElseThrow(() -> new IllegalStateException("Seed item SCARCE-SENSOR-CHILLER missing"));
    }

    @Test
    @DisplayName("Reserving parts with sufficient stock increases quantity_reserved and logs INVENTORY_RESERVED audit event")
    void testReservePartsSufficientStock() {
        int initialReserved = capacitorItem.getQuantityReserved() != null ? capacitorItem.getQuantityReserved() : 0;
        int qtyToReserve = 2;

        List<PartReservationItemDto> requestItems = List.of(
                new PartReservationItemDto(capacitorItem.getId(), qtyToReserve)
        );

        List<ReservationResponse> responses = inventoryService.reservePartsForJob(testJob.getId(), requestItems, "dispatcher");

        assertEquals(1, responses.size());
        ReservationResponse res = responses.get(0);
        assertEquals(testJob.getId(), res.getJobId());
        assertEquals(capacitorItem.getId(), res.getInventoryItemId());
        assertEquals(qtyToReserve, res.getQuantityReserved());
        assertEquals(ReservationStatus.RESERVED, res.getStatus());
        assertNotNull(res.getReservedAt());
        assertNull(res.getReleasedAt());

        // Verify inventory row was updated
        InventoryItem reloaded = inventoryItemRepository.findById(capacitorItem.getId()).orElseThrow();
        assertEquals(initialReserved + qtyToReserve, reloaded.getQuantityReserved());

        // Verify audit event
        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("INVENTORY", capacitorItem.getId());
        boolean hasReservedAudit = events.stream().anyMatch(e -> e.getAction() == AuditAction.INVENTORY_RESERVED);
        assertTrue(hasReservedAudit, "Expected AuditAction.INVENTORY_RESERVED event to be logged");
    }

    @Test
    @DisplayName("Reserving parts exceeding available stock throws InsufficientInventoryException and leaves stock untouched")
    void testReservePartsInsufficientStockThrowsException() {
        int onHand = scarceItem.getQuantityOnHand();
        int reserved = scarceItem.getQuantityReserved();
        int available = onHand - reserved;

        // Request 1 more than available
        int requested = available + 1;
        List<PartReservationItemDto> requestItems = List.of(
                new PartReservationItemDto(scarceItem.getId(), requested)
        );

        InsufficientInventoryException ex = assertThrows(InsufficientInventoryException.class, () ->
                inventoryService.reservePartsForJob(testJob.getId(), requestItems, "dispatcher")
        );

        assertEquals(scarceItem.getId(), ex.getItemId());
        assertEquals(scarceItem.getPartNumber(), ex.getPartNumber());
        assertEquals(requested, ex.getRequested());
        assertEquals(available, ex.getAvailable());

        // Verify no quantity reserved changed
        InventoryItem reloaded = inventoryItemRepository.findById(scarceItem.getId()).orElseThrow();
        assertEquals(reserved, reloaded.getQuantityReserved());
    }

    @Test
    @DisplayName("Releasing reservations restores quantity_reserved and marks reservation as RELEASED")
    void testReleaseReservations() {
        int initialReserved = capacitorItem.getQuantityReserved() != null ? capacitorItem.getQuantityReserved() : 0;
        int qty = 3;

        inventoryService.reservePartsForJob(testJob.getId(), List.of(
                new PartReservationItemDto(capacitorItem.getId(), qty)
        ), "dispatcher");

        // Now release
        List<ReservationResponse> released = inventoryService.releaseReservationsForJob(testJob.getId(), "Manual test release", "dispatcher");
        assertEquals(1, released.size());
        assertEquals(ReservationStatus.RELEASED, released.get(0).getStatus());
        assertNotNull(released.get(0).getReleasedAt());

        InventoryItem reloaded = inventoryItemRepository.findById(capacitorItem.getId()).orElseThrow();
        assertEquals(initialReserved, reloaded.getQuantityReserved());

        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("INVENTORY", capacitorItem.getId());
        boolean hasReleasedAudit = events.stream().anyMatch(e -> e.getAction() == AuditAction.INVENTORY_RELEASED);
        assertTrue(hasReleasedAudit, "Expected AuditAction.INVENTORY_RELEASED event to be logged");
    }

    @Test
    @DisplayName("Cancelling job triggers automatic inventory rollback releasing reserved parts")
    void testAutomaticRollbackOnJobCancellation() {
        int initialReserved = capacitorItem.getQuantityReserved() != null ? capacitorItem.getQuantityReserved() : 0;
        int qty = 2;

        inventoryService.reservePartsForJob(testJob.getId(), List.of(
                new PartReservationItemDto(capacitorItem.getId(), qty)
        ), "dispatcher");

        InventoryItem afterReserve = inventoryItemRepository.findById(capacitorItem.getId()).orElseThrow();
        assertEquals(initialReserved + qty, afterReserve.getQuantityReserved());

        // Cancel the job
        JobTransitionRequest cancelRequest = new JobTransitionRequest(JobStatus.CANCELLED, "Customer cancelled contract");
        jobService.transitionJob(testJob.getId(), cancelRequest, "dispatcher");

        // Verify reserved stock was automatically rolled back to initial
        InventoryItem afterCancel = inventoryItemRepository.findById(capacitorItem.getId()).orElseThrow();
        assertEquals(initialReserved, afterCancel.getQuantityReserved());

        // Verify reservation status is RELEASED
        List<ReservationResponse> reservations = inventoryService.getReservationsForJob(testJob.getId());
        assertEquals(1, reservations.size());
        assertEquals(ReservationStatus.RELEASED, reservations.get(0).getStatus());
    }

    @Test
    @DisplayName("Completing job permanently consumes reserved parts from quantity_on_hand")
    void testAutomaticConsumptionOnJobCompletion() {
        int initialOnHand = capacitorItem.getQuantityOnHand();
        int initialReserved = capacitorItem.getQuantityReserved() != null ? capacitorItem.getQuantityReserved() : 0;
        int qty = 2;

        inventoryService.reservePartsForJob(testJob.getId(), List.of(
                new PartReservationItemDto(capacitorItem.getId(), qty)
        ), "dispatcher");

        // Transition job: PENDING -> ASSIGNED -> EN_ROUTE -> IN_PROGRESS -> COMPLETED
        jobService.transitionJob(testJob.getId(), new JobTransitionRequest(JobStatus.ASSIGNED, null), "dispatcher");
        jobService.transitionJob(testJob.getId(), new JobTransitionRequest(JobStatus.EN_ROUTE, null), "dispatcher");
        jobService.transitionJob(testJob.getId(), new JobTransitionRequest(JobStatus.IN_PROGRESS, null), "dispatcher");
        jobService.transitionJob(testJob.getId(), new JobTransitionRequest(JobStatus.COMPLETED, "Repairs complete"), "dispatcher");

        // Verify stock consumed
        InventoryItem afterCompletion = inventoryItemRepository.findById(capacitorItem.getId()).orElseThrow();
        assertEquals(initialOnHand - qty, afterCompletion.getQuantityOnHand());
        assertEquals(initialReserved, afterCompletion.getQuantityReserved());

        // Verify reservation is CONSUMED
        List<ReservationResponse> reservations = inventoryService.getReservationsForJob(testJob.getId());
        assertEquals(1, reservations.size());
        assertEquals(ReservationStatus.CONSUMED, reservations.get(0).getStatus());

        List<AuditEvent> events = auditEventRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("INVENTORY", capacitorItem.getId());
        boolean hasConsumedAudit = events.stream().anyMatch(e -> e.getAction() == AuditAction.INVENTORY_CONSUMED);
        assertTrue(hasConsumedAudit, "Expected AuditAction.INVENTORY_CONSUMED event to be logged");
    }

    @Test
    @DisplayName("Attempting to reserve parts for a COMPLETED or CANCELLED job throws BusinessRuleException")
    void testCannotReserveForTerminalJob() {
        jobService.transitionJob(testJob.getId(), new JobTransitionRequest(JobStatus.CANCELLED, "Cancelled upfront"), "dispatcher");

        List<PartReservationItemDto> items = List.of(
                new PartReservationItemDto(capacitorItem.getId(), 1)
        );

        assertThrows(BusinessRuleException.class, () ->
                inventoryService.reservePartsForJob(testJob.getId(), items, "dispatcher")
        );
    }
}
