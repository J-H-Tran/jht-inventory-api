package com.jht.nvntry.catalog;

import com.jht.nvntry.catalog.model.CreateProductRequest;
import com.jht.nvntry.catalog.model.PatchProductRequest;
import com.jht.nvntry.catalog.model.Product;
import com.jht.nvntry.catalog.model.ProductResponse;
import com.jht.nvntry.shared.exception.ConflictException;
import com.jht.nvntry.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        String normalisedSku = sku.strip().toUpperCase();
        var product = productRepository.findBySku(normalisedSku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", normalisedSku));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listActive(Pageable pageable) {
        return productRepository.findAllActive(pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String normalisedSku = request.sku().strip().toUpperCase();
        if (productRepository.existsBySku(normalisedSku)) {
            throw new ConflictException("Product with SKU '" + normalisedSku + "' already exists");
        }
        Product product = Product.create(
                normalisedSku,
                request.name(),
                request.unitOfMeasure()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateName(UUID id, PatchProductRequest request) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        product.setName(request.name());
        /* Design issue worth noting — save() on dirty entities
         * In updateName and deactivate calling productRepository.save(product) explicitly after mutating the
         * entity. This works, but it's not why it works — and the distinction matters.
         *
         * Within a @Transactional method, Hibernate tracks every managed entity. When the transaction commits,
         * it flushes any mutations automatically — no save() needed. The explicit save() is redundant because
         * the entity is already managed.
         *
         * This matters beyond style. If habitually call save() on managed entities, eventually call it on
         * a detached entity (one loaded outside a transaction, mutated, then passed into a transactional method) and
         * get a silent second SELECT or unexpected behaviour. Understanding why you do or don't need save() is what
         * separates mechanical JPA use from actual understanding.
         *
         * The rule:
         * save() required → new entity (not yet persistent), or detached entity being re-attached
         * save() not required → managed entity mutated within the same transaction
         * */
        // No save() - Hibernate dirty checking flushes the mutation on commit
        // productRepository.save(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public void deactivate(UUID id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        product.setActive(false);
        // No save() - Hibernate dirty checking flushes the mutation on commit
        // productRepository.save(product);
    }
}
//    private final ProductRepository productRepository;
//
//    @Transactional(readOnly = true)
//    public ProductResponse getById(UUID id) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product ", id.toString()));
//        return ProductResponse.from(product);
//    }
//
//    @Transactional(readOnly = true)
//    public ProductResponse getBySku(String sku) {
//        Product product = productRepository.findBySku(sku.strip().toUpperCase())
//                .orElseThrow(() -> new ResourceNotFoundException("Product ", sku));
//        return ProductResponse.from(product);
//    }
//
//    @Transactional(readOnly = true)
//    public Page<ProductResponse> listActive(Pageable pageable) {
//        return productRepository.findAllActive(pageable)
//                .map(ProductResponse::from);
//    }
//
//    @Transactional
//    public ProductResponse create(CreateProductRequest req) {
//        String normalisedSku = req.sku().strip().toUpperCase();
//
//        if (productRepository.existsBySku(normalisedSku)) {
//            throw new ConflictException("Product with SKU '" + normalisedSku + "' already exists");
//        }
//
//        var product = Product.create(normalisedSku, req.name(), req.unitOfMeasure());
//        return ProductResponse.from(productRepository.save(product));
//    }
//
//    @Transactional
//    public ProductResponse updateName(UUID id, PatchProductRequest req) {
//        var product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product ", id.toString()));
//
//        product.setName(req.name().strip());
//        // No explicit save() needed - JPA dirty checking detects the mutation
//        // and issues the UPDATE on transaction commit. This is intentional.
//        return ProductResponse.from(product);
//    }
//
//    @Transactional
//    public void deactivate(UUID id) {
//        var product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Product ", id.toString()));
//        product.setActive(false);
//    }