package com.jht.nvntry.shared.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String key) {
        super("Movement with idempotency key '" + key + "' was already processed");
    }
}