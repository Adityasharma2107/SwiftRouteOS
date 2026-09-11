package com.swiftroute.controller;

import com.swiftroute.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health & Diagnostics", description = "System liveness and diagnostics endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Liveness probe", description = "Checks whether the SwiftRouteOS service is running")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> status = Map.of(
                "status", "UP",
                "service", "SwiftRouteOS Backend",
                "version", "v2.0.0",
                "engine", "Spring Boot 3.4.3"
        );
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
