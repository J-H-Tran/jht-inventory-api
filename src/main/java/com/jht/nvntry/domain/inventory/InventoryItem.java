package com.jht.nvntry.domain.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Deprecated
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
public class InventoryItem {
    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false, columnDefinition = "INT CHECK (quantity >= 0)")
    private int quantity;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(
            UUID productId,
            UUID locationId,
            int quantity,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}