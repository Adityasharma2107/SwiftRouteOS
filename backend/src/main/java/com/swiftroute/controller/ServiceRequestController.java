package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.dto.request.CreateServiceRequestDto;
import com.swiftroute.dto.response.ServiceRequestResponse;
import com.swiftroute.service.ServiceRequestService;
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
@RequestMapping("/api/service-requests")
@Tag(name = "Service Requests", description = "Customer service request intake and tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create Service Request",
               description = "Records incoming request, computes SLA response/resolution deadlines from policy, and generates an operational job")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestDto dto,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "system";
        ServiceRequestResponse response = serviceRequestService.createServiceRequest(dto, username);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Service request and operational job created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Service Request by ID")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getServiceRequestById(@PathVariable Long id) {
        ServiceRequestResponse response = serviceRequestService.getServiceRequestById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List Service Requests", description = "Lists service requests with optional status filter")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAllServiceRequests(
            @RequestParam(required = false) String status) {

        List<ServiceRequestResponse> response = serviceRequestService.getAllServiceRequests(status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
