package com.jht.nvntry.domain.inventory.dto;

import java.util.UUID;

public record InventoryAvailabilityDTO(
        UUID productId,
        UUID locationId,
        long available, // physically in stock
        long reserved   // sum of active reservations at this location
) {}