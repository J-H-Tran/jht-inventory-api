package com.jht.nvntry.movements;

import com.jht.nvntry.catalog.ProductRepository;
import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryLedgerResponse;
import com.jht.nvntry.shared.exception.BadRequestException;
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
    public InventoryLedgerResponse inventoryMovement(
            InventoryMovementRequest request
    ) { // throws BadRequestException for now, handle in @ControllerAdvice
        // 1. Load product -> throw ResourceNotFound if absent. Failure mode 1
        var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId().toString()));

        // 2. Validate active -> throw Insufficient 422 if inactive. Failure mode 2
        if (!product.isActive()) {
            throw new InactiveProductException(product);
        }

        // 3. Validate movement business rules -> reasonCode presence, sign of quantityDelta
        switch (request.movementType()) {
            case ADJUST -> {
                if (request.reasonCode() == null)
                    throw new BadRequestException(request.movementType() + " should have a reason code");
                if (request.quantityDelta() == 0)
                    throw new BadRequestException(request.movementType() + " should not have quantity delta of 0");
            }
            case RECEIVE -> {
                if (request.reasonCode() != null)
                    throw new BadRequestException(request.movementType() + " should not have a reason code");
                if (request.quantityDelta() <= 0)
                    throw new BadRequestException(request.movementType() + " should not have negative quantity delta");
            }
            case SHIP -> {
                if (request.reasonCode() != null)
                    throw new BadRequestException(request.movementType() + " should not have a reason code");
                if (request.quantityDelta() >= 0)
                    throw new BadRequestException(request.movementType() + " should not have positive quantity delta");
            }
        }

        // 4. Execute CTE -> atomic stock check + insert
        var result = ledgerRepository.insertWithStockCheck(
                request.productId(),
                request.movementType().name(), // Possible bug, call .name() and change repo type to String
                request.quantityDelta(),
                request.reasonCode(),
                request.referenceId(),
                request.referenceType(),
                request.idempotencyKey(),
                request.createdBy(),
                request.note()
        ).orElseThrow(() -> new InsufficientStockException(product.getSku(), 0, request.quantityDelta()));
        // 5. If CTE returns no rows -> throw Insufficient (stock went negative). Failure mode 3

        // 6. Map result -> return MovementResponse
        return InventoryLedgerResponse.from(result);
    }
}