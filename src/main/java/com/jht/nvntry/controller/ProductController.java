package com.jht.nvntry.controller;

import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("products")
public class ProductController {
    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    // browse products
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
    // view product details
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductDetails(@PathVariable("id") UUID productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    // check availability
    //@GetMapping("{id}/inventory?")
}