package com.jht.nvntry.api.event;

import lombok.Getter;
import java.util.UUID;

@Getter
public class MovementCreatedEvent {
    private final UUID productId;
    private final UUID locationId;

    public MovementCreatedEvent(
            UUID productId,
            UUID locationId
    )  {
        this.productId = productId;
        this.locationId = locationId;
    }
}