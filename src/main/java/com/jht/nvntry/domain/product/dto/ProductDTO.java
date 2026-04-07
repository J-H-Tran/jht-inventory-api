package com.jht.nvntry.domain.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String sku,
        String name,
        Instant createdAt,
        Instant updatedAt
) {}