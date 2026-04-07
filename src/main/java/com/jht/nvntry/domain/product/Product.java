package com.jht.nvntry.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Product() {
    }

    public Product(
            String sku,
            String name,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.name = name;
        this.createdAt = createdAt;
    }
}