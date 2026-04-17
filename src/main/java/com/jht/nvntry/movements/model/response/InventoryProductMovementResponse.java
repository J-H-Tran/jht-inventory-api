package com.jht.nvntry.movements.model.response;

import com.jht.nvntry.movements.model.InventoryLedger;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryProductMovementResponse(
        UUID id,
        UUID productId,
        InventoryLedger.MovementType movementType,
        int quantityDelta,
        String reasonCode,
        UUID referenceId,
        String referenceType,
        String idempotencyKey,
        OffsetDateTime occurredAt,
        String createdBy,
        String note
) {
    public static InventoryProductMovementResponse from(InventoryLedger i) {
        return new InventoryProductMovementResponse(
                i.getId(),
                i.getProductId(),
                i.getMovementType(),
                i.getQuantityDelta(),
                i.getReasonCode(),
                i.getReferenceId(),
                i.getReferenceType(),
                i.getIdempotencyKey(),
                i.getOccurredAt(),
                i.getCreatedBy(),
                i.getNote()
        );
    }
}