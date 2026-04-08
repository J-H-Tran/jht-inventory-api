package com.jht.nvntry.service.impl;

import com.jht.nvntry.api.exception.NotFoundException;
import com.jht.nvntry.domain.product.Product;
import com.jht.nvntry.domain.product.dto.ProductCreationDTO;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.dto.ProductModificationDTO;
import com.jht.nvntry.domain.product.mapper.ProductMapper;
import com.jht.nvntry.repository.ProductRepository;
import com.jht.nvntry.service.ProductService;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final Clock clock;

    public ProductServiceImpl (
            ProductRepository productRepository,
            ProductMapper mapper,
            Clock clock
    ) {
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public ProductDTO getProduct(UUID id) {
        Product product = getExistingProduct(id);
        return mapper.toDTO(product);
    }

    @Override
    public ProductDTO createProduct(ProductCreationDTO dto) {
        Product p = Product.createActive(
                dto.sku(),
                dto.name(),
                clock
        );

        Product savedProduct = productRepository.save(p);
        return mapper.toDTO(savedProduct);
    }

    @Override
    public ProductDTO updateProduct(UUID id, ProductModificationDTO dto) {
        Product productMod = mapper.toEntity(dto);

        Product product = getExistingProduct(id);
        // Only update if field is provided (not null)
        if (dto.sku() != null) product.setSku(productMod.getSku());
        if (dto.name() != null) product.setName(productMod.getName());
        product.setUpdatedAt(Instant.now(clock));

        Product savedProduct = productRepository.save(product);
        return mapper.toDTO(savedProduct);
    }

    private Product getExistingProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }
}