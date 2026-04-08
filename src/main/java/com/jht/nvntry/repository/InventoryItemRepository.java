package com.jht.nvntry.repository;

import com.jht.nvntry.domain.inventory.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Deprecated
@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByProductIdAndLocationId(UUID prodId, UUID locId);
}