package com.jht.nvntry.domain.inventory;

import com.jht.nvntry.domain.common.Reason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
public class InventoryMovement {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "reservation_id", nullable = true)
    private UUID reservationId;

    @Column(name = "change_amount", nullable = false)
    private int changeAmount;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private Reason reason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public InventoryMovement() {
    }

    public InventoryMovement(
            UUID productId,
            UUID locationId,
            UUID reservationId,
            int changeAmount,
            Reason reason
    ) {
        this.productId = productId;
        this.locationId = locationId;
        this.reservationId = reservationId;
        this.changeAmount = changeAmount;
        this.reason = reason;
    }
}