package com.jht.nvntry.domain.inventory;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.springframework.data.annotation.Immutable;

/** CurrentInventory
 * A read-only projection from MATERIALIZED VIEW
 *
 * Table name current_inventory maps to the MV
 * @Immutable important, tells Hibernate it's read-only
 * private quantity: from SUM(change_amount)
 * */
@Entity
@Table(name = "current_inventory")
@Immutable
@Getter
public class CurrentInventory {
    @EmbeddedId
    private InventoryKey id; // productId + locationId as composite
    private int quantity;
}