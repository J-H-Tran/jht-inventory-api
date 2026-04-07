package com.jht.nvntry.service;

import com.jht.nvntry.domain.product.dto.ProductDTO;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductDTO> getAllProducts();
    ProductDTO getProductById(UUID productId);
}