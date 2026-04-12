package com.jht.nvntry.catalog;

import com.jht.nvntry.catalog.model.request.CreateProductRequest;
import com.jht.nvntry.catalog.model.request.PatchProductRequest;
import com.jht.nvntry.catalog.model.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.UUID;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog management")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/{id}")
    @Operation(summary = "Get product details using id")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable("id") UUID id
    ) { // Get id: response 200 OK, input: Product id
        return ResponseEntity.ok().body(catalogService.getById(id));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(
            @PathVariable("sku") String sku
    ) { // Get sku: response 200 OK, input Product sku
        return ResponseEntity.ok().body(catalogService.getBySku(sku));
    }

    @GetMapping
    @Operation(summary = "List active products")
    public Page<ProductResponse> listActive(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) { // Search: response status 200 OK, input: filters (pageable)
        return catalogService.listActive(pageable);
    }

    @PostMapping
    @Operation(summary = "Register a new product")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request // @Valid enables validation annotations ie. @NotBlank, etc.
    ) { // Create: response status 201 CREATED, input: body w/ Product info
        var created = catalogService.create(request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/name")
    @Operation(summary = "Update product name")
    public ResponseEntity<ProductResponse> updateName(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PatchProductRequest request
    ) { // Update: response status 200 OK, input: UUID id + body w/ name
        return ResponseEntity.ok().body(catalogService.updateName(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a product (soft delete)")
    public ResponseEntity<ProductResponse> deactivate(
            @PathVariable("id") UUID id
    ) { // Delete: response status 204 NO CONTENT, input: Product id
        catalogService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
//    private final CatalogService catalogService;
//
//    @GetMapping
//    @Operation(summary = "List active products")
//    public Page<ProductResponse> listActive(
//            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
//    ) {
//        return catalogService.listActive(pageable);
//    }
//
//    @PostMapping
//    @Operation(summary = "Register a new product")
//    public ResponseEntity<ProductResponse> create(
//            @Valid @RequestBody CreateProductRequest req
//    ) {
//        var created = catalogService.create(req);
//        var location = ServletUriComponentsBuilder
//                .fromCurrentRequest()
//                .path("/{id}")
//                .buildAndExpand(created.id())
//                .toUri();
//        return ResponseEntity.created(location).body(created);
//    }
//
//    @PatchMapping("/{id}/name")
//    @Operation(summary = "Update product name")
//    public ProductResponse updateName(
//            @PathVariable("id") UUID id,
//            @RequestBody PatchProductRequest req
//    ) {
//        return catalogService.updateName(id, req);
//    }
//
//    @DeleteMapping("/{id}")
//    @Operation(summary = "Deactivate a product (soft delete)")
//    public ResponseEntity<ProductResponse> deactivate(
//            @PathVariable("id") UUID id
//    ) {
//        catalogService.deactivate(id);
//        return ResponseEntity.noContent().build();
//    }