package com.jht.nvntry.movements.model.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MovementHistoryResponse(
        List<InventoryProductMovementResponse> content,
        boolean hasMore,
        UUID nextCursor, // ID of last item returned, null if hasMore = false
        OffsetDateTime nextOccurredAt
) {}