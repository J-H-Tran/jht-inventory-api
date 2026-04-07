package com.jht.nvntry.domain.product.mapper;

import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.domain.product.dto.ProductCreationDTO;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.dto.ProductModificationDTO;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class ProductMapper {

    public ProductDTO toDTO(Product p) {
        if (p == null) return null;
        UUID id = p.getId();
        String sku = p.getSku();
        String name = p.getName();
        Instant createdAt = p.getCreatedAt();
        Instant updatedAt = p.getUpdatedAt();
        return new ProductDTO(id, sku, name, createdAt, updatedAt);
    }

    public Product toEntity(ProductCreationDTO dto) {
        if (dto == null) return null;
        Product p = new Product();
        p.setSku(dto.sku());
        p.setName(dto.name());
        return p;
    }

    public Product toEntity(ProductModificationDTO dto) {
        if (dto == null) return null;
        Product p = new Product();
        p.setSku(dto.sku());
        p.setName(dto.name());
        return p;
    }
}