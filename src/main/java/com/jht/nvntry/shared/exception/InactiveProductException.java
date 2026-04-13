package com.jht.nvntry.shared.exception;

import com.jht.nvntry.catalog.model.Product;

public class InactiveProductException extends RuntimeException {
    public InactiveProductException(Product product) {
        super("Product '" + product.getSku() + "' is inactive and cannot receive movements");
    }
}