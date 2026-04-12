package com.jht.nvntry.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires no-arg; protected prevents casual misuse
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false)
    @Setter
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measure", nullable = false, length = 30)
    private UnitOfMeasure unitOfMeasure;

    @Column(nullable = false)
    @Setter // Set false: business meaning -> soft delete
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /* Factory Pattern
     * No public constructor, creation goes through here so invariants are
     * enforced at the object level, not scattered across service code.
     * */
    public static Product create(
            String sku,
            String name,
            UnitOfMeasure unitOfMeasure
    ) {
        Product p = new Product();
        p.sku = sku.strip().toUpperCase();
        p.name = name.strip();
        p.unitOfMeasure = unitOfMeasure;
        p.active = true;
        p.createdAt = OffsetDateTime.now();
        p.updatedAt = OffsetDateTime.now();
        return p;
    }

    // Lifecycle
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Enum
    public enum UnitOfMeasure {
        EACH,
        KG,
        LITRE,
        METRE,
        BOX,
        PALLET
    }
}
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(updatable = false, nullable = false)
//    private UUID id;
//
//    @Column(nullable = false, unique = true, length = 100)
//    private String sku;
//
//    @Column(nullable = false, length = 255)
//    @Setter // name is the only field we allow post-creation mutation
//    private String name;
//
//    @Column(name = "unit_of_measure", nullable = false, length = 30)
//    @Enumerated(EnumType.STRING)
//    private UnitOfMeasure unitOfMeasure;
//
//    @Column(nullable = false)
//    @Setter
//    private boolean active;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private OffsetDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    private OffsetDateTime updatedAt;
//
//    /* Factory Pattern
//    * No public constructor, creation goes through here so invariants are
//    * enforced at the object level, not scattered across service code.
//    * */
//    public static Product create(
//            String sku,
//            String name,
//            UnitOfMeasure unitOfMeasure
//    ) {
//        Product product = new Product();
//        product.sku = sku.strip().toUpperCase();
//        product.name = name.strip();
//        product.unitOfMeasure = unitOfMeasure;
//        product.active = true;
//        product.createdAt = OffsetDateTime.now();
//        product.updatedAt = OffsetDateTime.now();
//        return product;
//    }
//
//    // Lifecycle
//    @PreUpdate
//    protected void onUpdate() {
//        this.updatedAt = OffsetDateTime.now();
//    }
//
//    // Enum
//    public enum UnitOfMeasure {
//        EACH,
//        KG,
//        LITRE,
//        METRE,
//        BOX,
//        PALLET
//    }