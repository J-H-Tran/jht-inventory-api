package com.jht.nvntry.domain.inventory.dto;

import java.util.UUID;

public record InventoryAvailabilityDTO(
        UUID productId,
        UUID locationId,
        int available, // physically in stock
        int reserved   // sum of active reservations at this location
) {}