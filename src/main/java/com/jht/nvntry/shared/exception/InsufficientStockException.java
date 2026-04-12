package com.jht.nvntry.shared.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for SKU '" + available + "' available, " + requested + " requested");
    }
}