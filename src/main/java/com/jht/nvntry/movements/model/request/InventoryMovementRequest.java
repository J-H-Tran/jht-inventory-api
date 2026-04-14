package com.jht.nvntry.movements.model.request;

import com.jht.nvntry.movements.model.InventoryLedger;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InventoryMovementRequest(

        @NotNull(message = "Product id is required")
        UUID productId,

        @NotNull(message = "Movement type is required")
        InventoryLedger.MovementType movementType,

        @NotNull(message = "Quantity delta is required")
        Integer quantityDelta,

        @Size(max = 50, message = "Reason code must not exceed 50 characters")
        String reasonCode,

        UUID referenceId,

        @Size(max = 30, message = "Reference type must not exceed 30 characters")
        String referenceType,

        @Size(max = 100, message = "Created by must not exceed 100 characters")
        String createdBy,

        String note
) {
}