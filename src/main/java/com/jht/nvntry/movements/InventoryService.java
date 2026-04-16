package com.jht.nvntry.movements;

import com.jht.nvntry.catalog.ProductRepository;
import com.jht.nvntry.movements.model.InventoryLedger;
import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import com.jht.nvntry.movements.model.response.InventoryProductMovementResponse;
import com.jht.nvntry.movements.model.response.InventoryStockResponse;
import com.jht.nvntry.movements.model.response.MovementHistoryResponse;
import com.jht.nvntry.shared.exception.BadRequestException;
import com.jht.nvntry.shared.exception.InactiveProductException;
import com.jht.nvntry.shared.exception.InsufficientStockException;
import com.jht.nvntry.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final LedgerRepository ledgerRepository;
    private final ProductRepository productRepository;
    private final AdjustmentReasonRepository adjustmentReasonRepository;

    @Transactional(readOnly = true)
    public InventoryStockResponse stockLevel(UUID id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        int currentStock = ledgerRepository.currentStockLevel(id);
        return new InventoryStockResponse(product.getId(), product.getSku(), currentStock, product.getUnitOfMeasure());
    }

    @Transactional
    public MovementHistoryResponse reportInventoryMovementHistory(
            UUID id,
            UUID lastSeenId,
            OffsetDateTime lastSeenOccurredAt,
            int limit
    ) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id.toString());
        }
        if (lastSeenId != null && lastSeenOccurredAt == null) {
            throw new IllegalArgumentException("Both cursor fields must be provided");
        }
        // Compute hasMore field indicator
        /* Your response already shows the problem. Look at what's in the response body:
         * {
         *    "pageable": { "offset": 0 ... },
         *    "total_elements": 3,
         *    "total_pages": 1
         * }
         *
         * Offset pagination works by saying "skip N rows, return M". Under concurrent inserts, a new ledger entry
         * inserted between page 1 and page 2 requests shifts every subsequent row — a client paging through movement
         * history can skip entries or see duplicates without knowing it. For a ledger that's being written to
         * continuously, this is a real correctness problem, not a theoretical one.
         *
         * Keyset pagination says "give me rows where id > lastSeenId" — the position is anchored to a specific row,
         * not a row count. Inserts between pages don't affect what you see.
         * */
        var entries = ledgerRepository.findByProductIdAfter(
            id,
            lastSeenId,
            lastSeenOccurredAt,
            PageRequest.of(0, limit + 1)
        ).stream()
        .map(InventoryProductMovementResponse::from)
        .collect(Collectors.toList());

        boolean hasMore = entries.size() > limit;
        var content = hasMore ? entries.subList(0, limit) : entries;
        UUID nextCursor = null;
        OffsetDateTime nextOccurredAt = null;

        if (hasMore) {
            var last = content.getLast();
            nextCursor = last.id();
            nextOccurredAt = last.occurredAt();
        }
        return new MovementHistoryResponse(content, hasMore, nextCursor, nextOccurredAt);
    }

    @Transactional
    public InventoryMovementResponse processInventoryMovement(
            String idempKey,
            InventoryMovementRequest request
    ) {
        // 1. Syntactic validation is handled by @Valid in Controller
        // 2. Semantic/Business validation: Context-dependent rules
        validateBusinessRules(request);

        if (request.movementType() == InventoryLedger.MovementType.ADJUST) {
            adjustmentReasonRepository.findByCode(request.reasonCode())
                    .orElseThrow(() -> new ResourceNotFoundException("AdjustmentReason", request.reasonCode()));
        }

        // 3. Load product -> throw ResourceNotFound if absent. Failure mode 1
        var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId().toString()));

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
}