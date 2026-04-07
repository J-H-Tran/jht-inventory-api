package com.jht.nvntry.domain.reservation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservation {
    public static enum Status {
        ACTIVE, FULFILLED, CANCELLED, EXPIRED
    }

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, columnDefinition = "INT CHECK (quantity > 0)")
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Reservation() {
    }

    public Reservation(
            UUID productId,
            long quantity,
            Status status,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.version = 0;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }
}