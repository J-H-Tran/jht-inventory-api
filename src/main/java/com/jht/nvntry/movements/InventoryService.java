package com.jht.nvntry.movements;

import com.jht.nvntry.catalog.ProductRepository;
import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import com.jht.nvntry.shared.exception.BadRequestException;
import com.jht.nvntry.shared.exception.InactiveProductException;
import com.jht.nvntry.shared.exception.InsufficientStockException;
import com.jht.nvntry.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final LedgerRepository ledgerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public InventoryMovementResponse processInventoryMovement(
            String idempKey,
            InventoryMovementRequest request
    ) {
        // 1. Syntactic validation is handled by @Valid in Controller
        // 2. Semantic/Business validation: Context-dependent rules
        validateBusinessRules(request);

        // 3. Load product -> throw ResourceNotFound if absent. Failure mode 1
        var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found, ID: " + request.productId()));

        // 4. Validate active -> throw Insufficient 422 if inactive. Failure mode 2
        if (!product.isActive()) {
            throw new InactiveProductException("Product '" + product.getSku() + "' is inactive and cannot receive movements");
        }

        // 5. Execute CTE -> atomic stock check + insert. Core: Atomic Ledger Update
        var ledgerEntry = ledgerRepository.insertWithStockCheck(
                request.productId(),
                request.movementType().name(),
                request.quantityDelta(),
                request.reasonCode(),
                request.referenceId(),
                request.referenceType(),
                idempKey,
                request.createdBy(),
                request.note()
        ).orElseThrow(() -> new InsufficientStockException("Insufficient stock for SKU '" + product.getSku() + "'"));
        // 6. If CTE returns no rows -> throw Insufficient (stock went negative). Failure mode 3

        // 7. Map result -> return MovementResponse
        return InventoryMovementResponse.from(ledgerEntry);
    }

    private void validateBusinessRules(InventoryMovementRequest request) {
        /* Guard: if core fields failed @NotNull, Bean Validation already registered those errors.
         * Exit here - further validation would NPE.
         * This is the correct pattern when combining Spring's Validator interface with Bean Validation.
         * The Validator runs after @Valid populates BindingResult, but it does not know which fields already
         * failed. Guarding against null before accessing fields that could be null is my responsibility.
         * */
        if (request.movementType() == null || request.quantityDelta() == null) {
            throw new BadRequestException("Movement type and quantity are required");
        }

        // Movement type specific rules
        switch(request.movementType()) {
            case ADJUST -> {
                if (request.reasonCode() == null || request.reasonCode().isBlank()) {
                    throw new BadRequestException(request.movementType().name() + " movement requires a reason code");
                }
                if (request.quantityDelta() == 0) {
                    throw new BadRequestException(request.movementType().name() + " movement must have non-zero quantity delta");
                }
            }
            case RECEIVE -> {
                if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
                    throw new BadRequestException(request.movementType().name() + " movement should not have a reason code");
                }
                if (request.quantityDelta() <= 0) {
                    throw new BadRequestException(request.movementType().name() + " movement must have positive quantity delta");
                }
            }
            case SHIP -> {
                if (request.reasonCode() != null && !request.reasonCode().isBlank()) {
                    throw new BadRequestException(request.movementType().name() + " movement should not have a reason code");
                }
                if (request.quantityDelta() >= 0) {
                    throw new BadRequestException(request.movementType().name() + " movement must have negative quantity delta");
                }
            }
        }

        // Reference consistency
        if ((request.referenceId() == null && request.referenceType() != null) ||
            (request.referenceId() != null && request.referenceType() == null)) {
            throw new BadRequestException("Both referenceId and referenceType must be present or both absent");
        }
    }
    // TODO: return 404 if reasonCode is supplied but does not exist in adjustment_reasons
    // TODO: return 422 if submitting SHIP would result in negative stock
}