package com.jht.nvntry.movements;

import com.jht.nvntry.catalog.ProductRepository;
import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import com.jht.nvntry.shared.exception.InactiveProductException;
import com.jht.nvntry.shared.exception.InsufficientStockException;
import com.jht.nvntry.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public InventoryMovementResponse inventoryMovement(
            String idempKey,
            InventoryMovementRequest request
    ) {
        // 1. Load product -> throw ResourceNotFound if absent. Failure mode 1
        var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId().toString()));

        // 2. Validate active -> throw Insufficient 422 if inactive. Failure mode 2
        if (!product.isActive()) {
            throw new InactiveProductException(product);
        }

        // 3. Execute CTE -> atomic stock check + insert
        var result = ledgerRepository.insertWithStockCheck(
                request.productId(),
                request.movementType().name(), // Possible bug, call .name() and change repo type to String
                request.quantityDelta(),
                request.reasonCode(),
                request.referenceId(),
                request.referenceType(),
                idempKey,
                request.createdBy(),
                request.note()
        ).orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.quantityDelta()));
        // 4. If CTE returns no rows -> throw Insufficient (stock went negative). Failure mode 3

        // 5. Map result -> return MovementResponse
        return InventoryMovementResponse.from(result);
    }
}