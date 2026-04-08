package com.jht.nvntry.domain.product.mapper;

import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.domain.product.dto.ProductCreationDTO;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.dto.ProductModificationDTO;
import org.springframework.stereotype.Component;
import java.time.Clock;

@Component
public class ProductMapper {
    private final Clock clock;

    public ProductMapper(Clock clock) {
        this.clock = clock;
    }

    public ProductDTO toDTO(Product p) {
        if (p == null) return null;
        return new ProductDTO(p.getId(), p.getSku(), p.getName(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public Product toEntity(ProductCreationDTO dto) {
        if (dto == null) return null;
        return Product.createActive(dto.sku(), dto.name(), clock);
    }

    public Product toEntity(ProductModificationDTO dto) {
        if (dto == null) return null;
        return Product.createActive(dto.sku(), dto.name(), clock);
    }
}