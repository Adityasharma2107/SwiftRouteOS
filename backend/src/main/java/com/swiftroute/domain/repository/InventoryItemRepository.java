package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByPartNumber(String partNumber);

    /**
     * PESSIMISTIC_WRITE Lock: Acquires a database-level 'SELECT ... FOR UPDATE' lock.
     * Guarantees transactional isolation and prevents race conditions / negative stock
     * when multiple concurrent dispatch requests attempt to reserve scarce items.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * Low stock threshold query: quantity_on_hand - quantity_reserved <= reorder_threshold.
     */
    @Query("""
        SELECT i FROM InventoryItem i
        WHERE (i.quantityOnHand - i.quantityReserved) <= i.reorderThreshold
    """)
    List<InventoryItem> findLowStockItems();
}
