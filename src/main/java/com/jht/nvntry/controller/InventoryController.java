package com.jht.nvntry.controller;

import com.jht.nvntry.service.InventoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService
    ) {
        this.inventoryService = inventoryService;
    }

/*    @PostMapping("/receipts")
    public ResponseEntity<> receiveStock() {
        // Input: ReceiptDTO (PO, items, qty)
        // Response: 201 Created
        // Triggers stock movement events
    }

    @PostMapping("/adjustments")
    public ResponseEntity<> adjustStock() {
        // Input: AdjustmentDTO (reason, delta)
        // Response: 201 Created
        // Requires approval workflow
    }*/
}