package com.jht.nvntry.service;

import com.jht.nvntry.domain.inventory.dto.InventoryAvailabilityDTO;
import java.util.UUID;

public interface InventoryService {
    InventoryAvailabilityDTO checkAvailability(UUID prodId, UUID locId);
}