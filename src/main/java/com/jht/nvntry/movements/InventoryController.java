package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1/inventory")
@Validated
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/movements")
    @Operation(summary = "Movement of a product")
    public ResponseEntity<InventoryMovementResponse> movement(
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header is required")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\-]{6,98}[A-Za-z0-9]$", message = "Invalid key format")
            String idempKey,
            @Valid @RequestBody InventoryMovementRequest request
    ) {
        var response = inventoryService.processInventoryMovement(idempKey, request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}