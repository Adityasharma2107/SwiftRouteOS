package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.domain.enums.JobStatus;
import com.swiftroute.domain.enums.Priority;
import com.swiftroute.domain.enums.SlaStatus;
import com.swiftroute.dto.request.JobTransitionRequest;
import com.swiftroute.dto.response.JobResponse;
import com.swiftroute.service.JobService;
import com.swiftroute.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Operational job management and SLA tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class JobController {

    private final ServiceRequestService serviceRequestService;
    private final JobService jobService;

    public JobController(ServiceRequestService serviceRequestService, JobService jobService) {
        this.serviceRequestService = serviceRequestService;
        this.jobService = jobService;
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

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition Job Status",
               description = "Applies state machine transition (EN_ROUTE, IN_PROGRESS, COMPLETED, CANCELLED) with operational side effects")
    public ResponseEntity<ApiResponse<JobResponse>> transitionJob(
            @PathVariable Long id,
            @Valid @RequestBody JobTransitionRequest request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "system";
        JobResponse response = jobService.transitionJob(id, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Job status transitioned successfully", response));
    }
}
