package com.jht.nvntry.service.impl;

import com.jht.nvntry.api.exception.NotFoundException;
import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.mapper.ProductMapper;
import com.jht.nvntry.repository.ProductRepository;
import com.jht.nvntry.service.ProductService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public ProductServiceImpl (
            ProductRepository productRepository,
            ProductMapper mapper
    ) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductDTO getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        return mapper.toDTO(product);
    }
}