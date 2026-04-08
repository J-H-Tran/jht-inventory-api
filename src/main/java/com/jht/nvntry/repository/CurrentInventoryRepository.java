package com.jht.nvntry.repository;

import com.jht.nvntry.domain.inventory.CurrentInventory;
import com.jht.nvntry.domain.inventory.InventoryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrentInventoryRepository extends JpaRepository<CurrentInventory, InventoryKey> {
}