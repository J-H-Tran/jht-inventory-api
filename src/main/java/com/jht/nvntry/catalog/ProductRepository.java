package com.jht.nvntry.catalog;

import com.jht.nvntry.catalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);

   /* Active-only listing - most callers never want inactive products
    * Named query so the intent is visible at the call site
    * */
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.active = TRUE
        ORDER BY p.createdAt DESC
    """)
    Page<Product> findAllActive(Pageable pageable);
}