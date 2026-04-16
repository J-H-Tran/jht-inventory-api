package com.jht.nvntry.movements;

import com.jht.nvntry.movements.model.request.InventoryMovementRequest;
import com.jht.nvntry.movements.model.response.InventoryMovementResponse;
import com.jht.nvntry.movements.model.response.InventoryStockResponse;
import com.jht.nvntry.movements.model.response.MovementHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/inventory")
@Validated
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/stock/{productId}")
    @Operation(summary = "Get current stock level for a product")
    public ResponseEntity<InventoryStockResponse> stock(
            @PathVariable("productId")UUID id
            ) {
        return ResponseEntity.ok(inventoryService.stockLevel(id));
    }

    @GetMapping("/movements")
    public ResponseEntity<MovementHistoryResponse> inventoryMovementHistory(
            @RequestParam("productId") UUID id,
            @RequestParam(required = false) UUID lastSeenId,
            @RequestParam(required = false) OffsetDateTime lastSeenOccurredAt,
            @RequestParam(defaultValue ="20") @Max(100) int limit
    ) {
        return ResponseEntity.ok(
                inventoryService.reportInventoryMovementHistory(
                        id, lastSeenId, lastSeenOccurredAt, limit
                )
        );
    }

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