package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.dto.response.SlaEscalationEventResponse;
import com.swiftroute.dto.response.SlaEvaluationResult;
import com.swiftroute.dto.response.SlaMetricsSummaryResponse;
import com.swiftroute.dto.response.SlaPolicyResponse;
import com.swiftroute.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sla")
@Tag(name = "SLA Management", description = "SLA policy monitoring, real-time compliance metrics, job escalations, and automated evaluation")
@SecurityRequirement(name = "Bearer Authentication")
public class SlaController {

    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get All SLA Policies", description = "Retrieves configured response and resolution SLA deadlines across priority levels")
    public ResponseEntity<ApiResponse<List<SlaPolicyResponse>>> getAllPolicies() {
        List<SlaPolicyResponse> policies = slaService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.ok("SLA policies fetched successfully", policies));
    }

    @GetMapping("/metrics/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get SLA Metrics Summary", description = "Calculates active jobs by SLA health status (HEALTHY, NEARING_BREACH, BREACHED) and overall compliance rate %")
    public ResponseEntity<ApiResponse<SlaMetricsSummaryResponse>> getMetricsSummary() {
        SlaMetricsSummaryResponse summary = slaService.getSlaMetricsSummary();
        return ResponseEntity.ok(ApiResponse.ok("SLA metrics summary fetched successfully", summary));
    }

    @GetMapping("/jobs/{jobId}/escalations")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'TECHNICIAN')")
    @Operation(summary = "Get Job SLA Escalation History", description = "Retrieves all recorded SLA escalation and warning events for a specific job")
    public ResponseEntity<ApiResponse<List<SlaEscalationEventResponse>>> getJobEscalations(@PathVariable Long jobId) {
        List<SlaEscalationEventResponse> events = slaService.getEscalationEventsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok("SLA escalation events fetched successfully", events));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Trigger Batch SLA Evaluation", description = "Manually triggers an immediate sweep and escalation check across all active jobs")
    public ResponseEntity<ApiResponse<Integer>> triggerBatchEvaluation() {
        int evaluatedCount = slaService.evaluateAndEscalateActiveJobs();
        return ResponseEntity.ok(ApiResponse.ok("Batch SLA evaluation completed: " + evaluatedCount + " active jobs processed", evaluatedCount));
    }

    @PostMapping("/jobs/{jobId}/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Evaluate Single Job SLA", description = "Evaluates a specific job's SLA compliance and triggers escalation events if thresholds are exceeded")
    public ResponseEntity<ApiResponse<SlaEvaluationResult>> evaluateJob(@PathVariable Long jobId) {
        SlaEvaluationResult result = slaService.processJobEscalation(jobId, Instant.now());
        return ResponseEntity.ok(ApiResponse.ok("Job SLA evaluated successfully", result));
    }
}
