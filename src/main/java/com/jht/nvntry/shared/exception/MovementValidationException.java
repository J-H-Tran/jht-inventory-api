package com.jht.nvntry.shared.exception;

import lombok.Getter;
import org.springframework.validation.BindingResult;

@Getter
public class MovementValidationException extends RuntimeException {
    private final BindingResult bindingResult;

    public MovementValidationException(BindingResult bindingResult) {
        super("Validation failed");
        this.bindingResult = bindingResult;
    }
}