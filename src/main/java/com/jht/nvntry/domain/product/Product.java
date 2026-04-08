package com.jht.nvntry.domain.product;

import com.jht.nvntry.domain.common.Status;
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
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class) // Tells driver to bind string value using Types.OTHER (the proper way for Postgres enums).
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product() {}

    public static Product createActive(
            String sku,
            String name,
            Clock clock
    ) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory("NONE");
        p.setStatus(Status.ACTIVE);
        Instant now = Instant.now(clock);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }
}