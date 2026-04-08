package com.jht.nvntry.domain.reservation;

import com.jht.nvntry.domain.common.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservation {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false, columnDefinition = "INT CHECK (quantity > 0)")
    private int quantity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    private Status status;

    @Version
    @Column(nullable = false)
    private int version = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Reservation() {
    }
    // Replace the messy constructors with a factory method or a single proper one
    public static Reservation createActive(
            UUID productId,
            UUID locationId,
            int quantity,
            Clock clock
    ) {
        Reservation r = new Reservation();
        r.setProductId(productId);
        r.setLocationId(locationId);
        r.setQuantity(quantity);
        r.setStatus(Status.ACTIVE);
        Instant now = Instant.now(clock);
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        r.setExpiresAt(now.plus(15, ChronoUnit.MINUTES)); // make duration configurable
        return r;
    }
}