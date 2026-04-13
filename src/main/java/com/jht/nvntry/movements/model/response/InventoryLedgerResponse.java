package com.jht.nvntry.movements.model.response;

import com.jht.nvntry.movements.model.InventoryLedger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryLedgerResponse(

        @Schema(description = "Unique Ledger ID")
        UUID id,
        UUID productId,
        InventoryLedger.MovementType movementType,
        int quantityDelta,
        UUID referenceId,
        String referenceType,
        String idempotencyKey,
        OffsetDateTime occurredAt,
        String createdBy,
        String note
) {
    public static InventoryLedgerResponse from(InventoryLedger i) {
        return new InventoryLedgerResponse(
                i.getId(),
                i.getProductId(),
                i.getMovementType(),
                i.getQuantityDelta(),
                i.getReferenceId(),
                i.getReferenceType(),
                i.getIdempotencyKey(),
                i.getOccurredAt(),
                i.getCreatedBy(),
                i.getNote()
        );
    }
}