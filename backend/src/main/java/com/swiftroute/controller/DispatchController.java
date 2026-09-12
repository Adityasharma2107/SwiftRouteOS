package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import com.swiftroute.dto.response.DispatchRecommendationResponse;
import com.swiftroute.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispatch")
@Tag(name = "Dispatch & Recommendations", description = "Intelligent technician candidate scoring, ranking, and explainability")
@SecurityRequirement(name = "Bearer Authentication")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping("/recommendations/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get Dispatch Candidate Recommendations",
               description = "Applies hard filter constraints, calculates Haversine proximity, computes multi-factor weighted scores, and ranks eligible technicians with explainability")
    public ResponseEntity<ApiResponse<DispatchRecommendationResponse>> getRecommendations(@PathVariable Long jobId) {
        DispatchRecommendationResponse response = dispatchService.getRecommendationsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate recommendations generated successfully", response));
    }
}
