package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import com.jht.nvntry.shared.exception.MovementValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management")
public class InventoryController {

    private final LedgerService ledgerService;
    private final InventoryMovementValidator movementValidator;

    @PostMapping("/movements")
    @Operation(summary = "Movement of a product")
    public ResponseEntity<InventoryMovementResponse> movement(
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header is required")
            @Pattern(regexp = "^[A-Za-z0-9\\-]{8,100}$", message = "Idempotency-Key must be 8-100 alphanumeric characters")
            String idempKey,
            @Valid @RequestBody InventoryMovementRequest request,
            BindingResult bindingResult // Enables validation trigger
    ) {
        // Custom validation
        movementValidator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new MovementValidationException(bindingResult);
        }

        var response = ledgerService.inventoryMovement(idempKey, request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}