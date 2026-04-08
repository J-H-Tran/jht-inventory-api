package com.jht.nvntry.service;

import com.jht.nvntry.domain.inventory.dto.ReserveQuantityDTO;
import com.jht.nvntry.domain.reservation.dto.ReservationDTO;
import java.util.UUID;

public interface InventoryService {
    ReservationDTO reserve(UUID prodId, UUID locId, ReserveQuantityDTO dto);
}