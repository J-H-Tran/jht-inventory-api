package com.jht.nvntry.service;

import com.jht.nvntry.domain.product.dto.ProductCreationDTO;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.dto.ProductModificationDTO;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductDTO> getAllProducts();
    ProductDTO getProduct(UUID id);
    ProductDTO createProduct(ProductCreationDTO dto);
    ProductDTO updateProduct(UUID id, ProductModificationDTO dto);
}