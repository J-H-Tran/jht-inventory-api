package com.jht.nvntry.catalog;

import com.jht.nvntry.catalog.model.Product;
import com.jht.nvntry.catalog.model.request.CreateProductRequest;
import com.jht.nvntry.catalog.model.request.PatchProductRequest;
import com.jht.nvntry.catalog.model.response.ProductResponse;
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

    /* Not just a hint — it tells Hibernate to skip dirty checking on loaded entities (a genuine performance saving) and
     * tells connection pool the connection doesn't need write capability. Habit worth building now.
     * */
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        // 1. Check DB if already exists
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        // 2. Return data found
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        String normalisedSku = sku.strip().toUpperCase();
        // 1. Check DB if already exists
        var product = productRepository.findBySku(normalisedSku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", normalisedSku));
        // 2. Return data found
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listActive(Pageable pageable) {
        // 1. Return data found, could be empty list -> not an error case
        return productRepository.findAllActive(pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String normalisedSku = request.sku().strip().toUpperCase();
        // 1. Check DB if already exists
        if (productRepository.existsBySku(normalisedSku)) {
            throw new ConflictException("Product with SKU '" + normalisedSku + "' already exists");
        }
        // 2. Set data to be saved
        Product product = Product.create(
                normalisedSku,
                request.name(),
                request.unitOfMeasure()
        );
        // 3. Save to DB, return data found
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateName(UUID id, PatchProductRequest request) {
        // 1. Check DB if already exists
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        // 2. Update data
        product.setName(request.name());
        /* Design issue worth noting — save() on dirty entities
         * In updateName and deactivate calling productRepository.save(product) explicitly after mutating the
         * entity. This works, but it's not why it works — and the distinction matters.
         *
         * Within a @Transactional method, Hibernate tracks every managed entity. When the transaction commits,
         * it flushes any mutations automatically — no save() needed. The explicit save() is redundant because
         * the entity is already managed.
         *
         * Flush - Hibernate synchronizes its in-memory session state with the database.
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
        // 2. Return data found
        return ProductResponse.from(product);
    }

    /* @Transactional method starts
     * → Hibernate Session opens
     *
     * product.setName("New Name")  ← Entity modified in memory
     * (no SQL executed yet!)
     *
     * Method ends / Transaction commits
     * → FLUSH: UPDATE sql sent to database
     * → COMMIT: transaction made permanent
     * */
    @Transactional
    public void deactivate(UUID id) {
        // 1. Check DB if already exists
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        // 2. Update data
        product.setActive(false);
        // No save() - Hibernate dirty checking flushes the mutation on commit
        // productRepository.save(product);
    }
}