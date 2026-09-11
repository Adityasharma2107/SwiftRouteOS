package com.swiftroute;

import com.swiftroute.domain.entity.*;
import com.swiftroute.domain.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SwiftRouteApplicationTests {

    @Test
    @DisplayName("Verify BCrypt password hashing and matching")
    void verifyBCryptPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("password123");
        assertTrue(encoder.matches("password123", hash));
    }

    @Test
    @DisplayName("Verify InventoryItem available quantity calculation")
    void verifyInventoryAvailableCalculation() {
        InventoryItem item = new InventoryItem(
                "TEST-PART-01",
                "Test Capacitor",
                "HVAC",
                10,
                3,
                2,
                BigDecimal.valueOf(25.50),
                0L
        );

        assertEquals(7, item.getAvailableQuantity());
    }

    @Test
    @DisplayName("Verify Job and SLA domain models instantiate properly")
    void verifyJobAndSlaModels() {
        Instant now = Instant.now();
        Instant deadline = now.plus(4, ChronoUnit.HOURS);

        Job job = new Job(
                null,
                Priority.CRITICAL,
                JobStatus.PENDING,
                now.plus(1, ChronoUnit.HOURS),
                deadline,
                SlaStatus.HEALTHY
        );

        assertEquals(Priority.CRITICAL, job.getPriority());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(SlaStatus.HEALTHY, job.getSlaStatus());
        assertFalse(job.getSlaResolutionDeadline().isBefore(now));
    }
}
