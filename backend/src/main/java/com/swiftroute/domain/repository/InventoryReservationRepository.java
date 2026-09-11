package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.InventoryReservation;
import com.swiftroute.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByJobId(Long jobId);
    List<InventoryReservation> findByJobIdAndStatus(Long jobId, ReservationStatus status);
}
