package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.InventoryLedger;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<InventoryLedger, UUID> {
    @Query("""
        SELECT i
        FROM InventoryLedger i
        WHERE i.productId = :productId
        AND (
            :lastSeenId IS NULL OR
            i.occurredAt < :lastSeenOccurredAt OR
            (i.occurredAt = :lastSeenOccurredAt AND i.id < :lastSeenId)
        )
        ORDER BY i.occurredAt DESC, i.id DESC
    """)
    List<InventoryLedger> findByProductIdAfterAndOccurredAtAfter(
            @Param("productId") UUID productId,
            @Param("lastSeenId") UUID lastSeenId,
            @Param("lastSeenOccurredAt") OffsetDateTime lastSeenOccurredAt,
            Pageable pageable
    );

    /* Two concurrent requests can both pass the check before either inserts, resulting in negative stock.
     * How This Query Fixes It.
     * The query uses PostgreSQL's atomic transaction to combine three operations:
     *   Step 1: Calculate what stock WOULD be after this change
     *     Calculate projected stock (current sum + new delta)
     *   Step 2: Only proceed if projected stock is non-negative
     *     Conditionally insert only if projected >= 0 -> business rule
     *   Step 3: Returns inserted row if successful, empty set if not
     *     Return the inserted row (or nothing if condition fails)
     *
     * The Problem It Solves
     * If you do this in application code:
     * // DANGEROUS - Race condition exists
     * int currentStock = ledgerRepository.sumByProduct(productId);
     * if (currentStock + delta >= 0) {
     *     ledgerRepository.insert(entry);  // Another request could slip in here
     * }
     * Two concurrent requests can both pass the check before either inserts, resulting in negative stock.
     * */
    @Query(value = """
        WITH stock_check AS (
            SELECT coalesce(sum(quantity_delta), 0) + :quantityDelta AS projected
            FROM inventory_ledger
            WHERE product_id = :productId
        )
        INSERT INTO inventory_ledger(
            id,
            product_id,
            movement_type,
            quantity_delta,
            reason_code,
            reference_id,
            reference_type,
            idempotency_key,
            occurred_at,
            created_by,
            note
        )
        SELECT gen_random_uuid(), :productId, cast(:movementType AS VARCHAR), :quantityDelta, :reasonCode,
            cast(:referenceId AS UUID), cast(:referenceType AS VARCHAR), :idempotencyKey, now(), :createdBy, :note
        FROM stock_check
        WHERE projected >= 0
        RETURNING *
    """, nativeQuery = true)
    Optional<InventoryLedger> insertWithStockCheck(
            @Param("productId") UUID productId,
            @Param("movementType") String movementType,
            @Param("quantityDelta") int quantityDelta,
            @Param("reasonCode") String reasonCode,
            @Param("referenceId") UUID referenceId,
            @Param("referenceType") String referenceType,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("createdBy") String createdBy,
            @Param("note") String note
    );

    @Query("""
        SELECT coalesce(sum(i.quantityDelta), 0)
        FROM InventoryLedger i
        WHERE i.productId = :productId
    """)
    int currentStockLevel(@Param("productId") UUID productId);
}