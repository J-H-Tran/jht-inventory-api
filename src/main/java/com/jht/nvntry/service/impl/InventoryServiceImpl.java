package com.jht.nvntry.service.impl;

import com.jht.nvntry.api.exception.NotFoundException;
import com.jht.nvntry.domain.inventory.InventoryItem;
import com.jht.nvntry.domain.inventory.dto.InventoryAvailabilityDTO;
import com.jht.nvntry.domain.location.Location;
import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.repository.InventoryItemRepository;
import com.jht.nvntry.repository.LocationRepository;
import com.jht.nvntry.repository.ProductRepository;
import com.jht.nvntry.repository.ReservationRepository;
import com.jht.nvntry.service.InventoryService;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public InventoryServiceImpl (
            ProductRepository productRepository,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            ReservationRepository reservationRepository,
            Clock clock
    ) {
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Override
    public InventoryAvailabilityDTO checkAvailability(UUID prodId, UUID locId) {
        // Verify product exists
        getExistingProduct(prodId);

        // Verify location exists
        getExistingLocation(locId);

        // Get physical stock at location
        InventoryItem inventory = inventoryItemRepository.findByProductIdAndLocationId(prodId, locId)
                .orElse(
                        new InventoryItem(prodId, locId, 0L, Instant.now(clock), Instant.now(clock))
                );

        // Get reserved quantity at this location
        long reserved = reservationRepository.sumActiveReservationsByproductAndLocation(prodId, locId);

        return new InventoryAvailabilityDTO(
                prodId,
                locId,
                inventory.getQuantity(),
                reserved
        );
    }

    private Product getExistingProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private Location getExistingLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Location not found: " + id));
    }
}