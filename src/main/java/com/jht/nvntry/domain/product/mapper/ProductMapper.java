package com.jht.nvntry.domain.product.mapper;

import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class ProductMapper {

    public ProductDTO toDTO(Product p) {
        if (p == null) return null;
//        UUID id = p.getId();
        String sku = p.getSku();
        String name = p.getName();
        Instant createdAt = p.getCreatedAt();
        return new ProductDTO(sku, name, createdAt);
    }

    public Product toProduct(ProductDTO dto) {
        Product p = new Product();
        return p;
    }
}