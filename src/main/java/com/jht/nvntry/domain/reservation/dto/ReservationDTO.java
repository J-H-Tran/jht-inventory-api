package com.jht.nvntry.domain.reservation.dto;

import java.util.UUID;

public record ReservationDTO(
        UUID productId,
        UUID locationId,
        int quantity
) {}