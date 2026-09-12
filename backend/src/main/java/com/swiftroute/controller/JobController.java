package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
import com.swiftroute.dto.response.JobResponse;
import com.swiftroute.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Operational job management and SLA tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class JobController {

    private final ServiceRequestService serviceRequestService;

    public JobController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Job by ID", description = "Retrieves job details including SLA deadlines, assignment, and parts")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id) {
        JobResponse response = serviceRequestService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List Jobs", description = "Retrieves operational jobs with optional status, priority, and SLA status filters")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) SlaStatus slaStatus) {

        List<JobResponse> response = serviceRequestService.getAllJobs(status, priority, slaStatus);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
