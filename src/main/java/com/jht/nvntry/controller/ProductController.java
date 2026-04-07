package com.jht.nvntry.controller;

import com.jht.nvntry.domain.inventory.dto.InventoryAvailabilityDTO;
import com.jht.nvntry.domain.product.dto.ProductCreationDTO;
import com.jht.nvntry.domain.product.dto.ProductDTO;
import com.jht.nvntry.domain.product.dto.ProductModificationDTO;
import com.jht.nvntry.service.InventoryService;
import com.jht.nvntry.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("products")
public class ProductController {
    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(
            ProductService productService,
            InventoryService inventoryService
    ) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductDetails(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductCreationDTO dto) {
        ProductDTO created = productService.createProduct(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable("id") UUID id,
            @RequestBody ProductModificationDTO dto
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    // check availability
    @GetMapping("{id}/inventory")
    public ResponseEntity<InventoryAvailabilityDTO> checkAvailability(
            @PathVariable("id") UUID prodId,
            @RequestParam("location") UUID locId
    ) {
        return ResponseEntity.ok(inventoryService.checkAvailability(prodId, locId));
    }
}