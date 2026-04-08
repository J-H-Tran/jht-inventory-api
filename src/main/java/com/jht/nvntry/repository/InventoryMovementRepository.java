package com.jht.nvntry.repository;

import com.jht.nvntry.domain.inventory.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;


@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    /* Why FOR UPDATE here?
     *
     * It acquires a row-level lock on all matching movement rows for that product/location.
     * Other transactions trying to reserve the same item will wait (or timeout) until this transaction commits.
     * This prevents the classic check-then-act race without needing full table locks.
     *
     * Strong pessimistic guard: lock the movements for this product+location while checking sum
     * */
    @Query(value = """
        SELECT change_amount
        FROM inventory_movements
        WHERE product_id = :productId
            AND location_id = :locationId
        FOR UPDATE
    """, nativeQuery = true)
    List<Integer> lockMovements(
            @Param("productId") UUID productId,
            @Param("locationId") UUID locationId
    );

    // Handling aggregates with pessimistic locking.
    default int getLockedAvailableQuantity(UUID productId, UUID locationId) {
        List<Integer> amounts = lockMovements(productId, locationId);
        return amounts.stream().mapToInt(Integer::intValue).sum();
    }
}