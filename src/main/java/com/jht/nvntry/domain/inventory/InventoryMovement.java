package com.jht.nvntry.domain.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
public class InventoryMovement {
    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "reservation_id", nullable = true)
    private UUID reservationId;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    public InventoryMovement() {
    }

    public InventoryMovement(
            UUID productId,
            UUID reservationId,
            long changeAmount,
            String reason,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.reservationId = reservationId;
        this.changeAmount = changeAmount;
        this.reason = reason;
        this.createdAt = createdAt;
    }
}