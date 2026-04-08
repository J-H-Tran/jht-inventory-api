package com.jht.nvntry.domain.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
public class Location {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Location() {
    }

    public Location(
            String code,
            String name,
            Instant updatedAt
    ) {
        this.code = code;
        this.name = name;
        this.updatedAt = updatedAt;
    }
}