package com.jht.nvntry.movements.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

// Immutable - No setters
@Entity
@Table(name = "inventory_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /* productId as UUID instead of a @ManyToOne — worth a conscious decision
     * You've mapped product_id as a plain UUID column rather than a JPA relationship. This is a legitimate choice — it
     * keeps the ledger decoupled from the Product entity, which aligns with the modulith boundary (the movements module
     * shouldn't pull in the catalog module's entity). But it should be a deliberate decision, not a default.
     * The tradeoff: you lose JPA's ability to join-fetch the product in one query, but you gain a clean module boundary
     * and avoid Hibernate lazy-loading surprises on an append-only table. For this design, the UUID reference is the
     * right call — just be able to articulate why
     * */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_by")
    private String createdBy;

    private String note;

    // Static Factory
    public static InventoryLedger create(
            UUID productId,
            MovementType movementType,
            int quantityDelta,
            String reasonCode,
            UUID referenceId,
            String referenceType,
            String idempotencyKey,
            OffsetDateTime occurredAt,
            String createdBy,
            String note
    ) {
        InventoryLedger ledger = new InventoryLedger();
        ledger.productId = productId;
        ledger.movementType = movementType;
        ledger.quantityDelta = quantityDelta;
        ledger.reasonCode = reasonCode;
        ledger.referenceId = referenceId;
        ledger.referenceType = referenceType;
        ledger.idempotencyKey = idempotencyKey;
        ledger.occurredAt = occurredAt != null ? occurredAt : OffsetDateTime.now();
        ledger.createdBy = createdBy;
        ledger.note = note;
        return ledger;
    }

    public enum MovementType {
        RECEIVE,
        SHIP,
        ADJUST
    }
}