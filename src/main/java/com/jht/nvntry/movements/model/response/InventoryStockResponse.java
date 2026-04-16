package com.jht.nvntry.movements.model.response;

import com.jht.nvntry.catalog.model.Product;

import java.util.UUID;

public record InventoryStockResponse(
        UUID productId,
        String sku,
        int currentStock,
        Product.UnitOfMeasure unitOfMeasure
) {}