package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.dto.request.ReserveInventoryRequest;
import com.swiftroute.dto.response.ChaosTestResultResponse;
import com.swiftroute.dto.response.InventoryItemResponse;
import com.swiftroute.dto.response.ReservationResponse;
import com.swiftroute.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory & Concurrency", description = "Concurrency-safe parts reservation, stock levels, and chaos concurrency testing")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'TECHNICIAN')")
    @Operation(summary = "Get All Inventory Items", description = "Fetches catalog items with on-hand, reserved, and available stock levels")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getAllItems() {
        List<InventoryItemResponse> items = inventoryService.getAllInventoryItems();
        return ResponseEntity.ok(ApiResponse.ok("Inventory items fetched successfully", items));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'TECHNICIAN')")
    @Operation(summary = "Get Inventory Item By ID", description = "Fetches item details and stock metrics")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> getItemById(@PathVariable Long id) {
        InventoryItemResponse item = inventoryService.getInventoryItemById(id);
        return ResponseEntity.ok(ApiResponse.ok("Inventory item fetched successfully", item));
    }

    @GetMapping("/items/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get Low Stock Items", description = "Fetches items where available quantity is at or below reorder threshold")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getLowStockItems() {
        List<InventoryItemResponse> items = inventoryService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.ok("Low stock inventory items fetched successfully", items));
    }

    @GetMapping("/jobs/{jobId}/reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'TECHNICIAN')")
    @Operation(summary = "Get Job Inventory Reservations", description = "Fetches active and completed part reservations for a job")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getJobReservations(@PathVariable Long jobId) {
        List<ReservationResponse> reservations = inventoryService.getReservationsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok("Job reservations fetched successfully", reservations));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Reserve Parts for Job", description = "Locks requested parts with pessimistic write lock (SELECT ... FOR UPDATE) and marks them RESERVED")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> reserveParts(
            @Valid @RequestBody ReserveInventoryRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        List<ReservationResponse> reservations = inventoryService.reservePartsForJob(request.getJobId(), request.getItems(), username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Parts reserved successfully", reservations));
    }

    @PostMapping("/jobs/{jobId}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Release Reservations for Job", description = "Releases active reservations and restores reserved quantities back to available stock")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> releaseReservations(
            @PathVariable Long jobId,
            @RequestParam(required = false, defaultValue = "Dispatcher manual release") String reason,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        List<ReservationResponse> released = inventoryService.releaseReservationsForJob(jobId, reason, username);
        return ResponseEntity.ok(ApiResponse.ok("Reservations released successfully", released));
    }

    @PostMapping("/chaos-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Run Multi-Threaded Chaos Concurrency Test",
               description = "Spawns concurrent worker threads to race for scarce inventory items, verifying zero oversell under pessimistic locking")
    public ResponseEntity<ApiResponse<ChaosTestResultResponse>> runChaosTest(
            @RequestParam(defaultValue = "10") int threads,
            @RequestParam(defaultValue = "true") boolean resetAfterTest
    ) {
        ChaosTestResultResponse result = inventoryService.runChaosSimulation(threads, resetAfterTest);
        return ResponseEntity.ok(ApiResponse.ok("Chaos concurrency simulation completed", result));
    }
}
