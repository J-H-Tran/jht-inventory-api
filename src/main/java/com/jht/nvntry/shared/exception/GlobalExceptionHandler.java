package com.jht.nvntry.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "/errors/not-found", ex.getMessage(), req);
    }

    // 409
    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "/errors/conflict", ex.getMessage(), req);
    }

    /* 409 Idempotency
    * Returned when the same idempotency key is submitted more than once.
    * 200 vs 409 debate: we return 409 here to signal "this was a duplicate".
    * A 200 would be correct if we stored and returned the original response.
    * That is a Week 2 enhancement - for now, reject cleanly.
    * */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // Unique constraint on idempotency_key is the only current source of this
        return problem(HttpStatus.CONFLICT, "/errors/duplicate-movement",
                "A movement with this idempotency key already exists", req);
    }

    /* 422
    * InsufficientStock is not a 400 (request was valid) nor a 409 (no resource conflict).
    * Is it a business rule violation on otherwise valid input - hence 422 Unprocessable.
    * */
    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/insufficient-stock", ex.getMessage(), req);
    }

    @ExceptionHandler(InactiveProductException.class)
    ProblemDetail handleInactiveProduct(InactiveProductException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/inactive-product", ex.getMessage(), req);
    }

    // 400 (Validation)
    @ExceptionHandler(MovementValidationException.class)
    ProblemDetail handleValidation(MovementValidationException ex, HttpServletRequest req) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a // keep first message per field
                ));
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation-failed", "Request validation failed", req);
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/missing-header", "Required request header is missing", req);
        pd.setProperty("fieldErrors", Map.of(
                ex.getHeaderName(), "Header is required"
        ));
        return pd;
    }

    @ExceptionHandler(MissingRequestValueException.class)
    ProblemDetail handleMissingRequestValue(MissingRequestValueException ex, HttpServletRequest req) {
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/missing-value", "Required request value is missing", req);
        return pd;
    }

    // If you use @NotBlank on headers, also add:
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        var fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a
                ));

        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation-failed",
                "Validation failed", req);
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation-failed", "Request body is malformed or contains invalid values", req);
        // Extract the root cause message
        Throwable cause = ex.getRootCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();
        // Parse common Jackson errors
        Map<String, String> fieldErrors = new HashMap<>();

        if (message.contains("Cannot deserialize value of type")) {
            // Extract field name from error message
            // Example: "Cannot deserialize value of type `...MovementType` from String \"invalid\": not one of...")
            if (message.contains("MovementType")) {
                fieldErrors.put("movementType", "Invalid movement type. Must be RECEIVE, SHIP, or ADJUST");
            }
        }
        if (message.contains("UUID")) {
            fieldErrors.put("productId", "Invalid UUID format");
        }

        if (fieldErrors.isEmpty()) {
            pd.setProperty("error", message);
        } else {
            pd.setProperty("fieldErrors", fieldErrors);
        }
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        var pd = problem(HttpStatus.BAD_REQUEST, "errors/validation-failed", "Validation failed", req);

        // Extract which field from message
        String message = ex.getMessage();
        Map<String, String> fieldErrors = new HashMap<>();

        if (message.contains("Product id")) {
            fieldErrors.put("productId", message);
        } else if (message.contains("Movement type")) {
            fieldErrors.put("movementType", message);
        } else if (message.contains("Quantity delta")) {
            fieldErrors.put("quantityDelta", message);
        } else {
            pd.setProperty("error", message);
        }
        if (!fieldErrors.isEmpty()) {
            pd.setProperty("fieldErrors", fieldErrors);
        }
        return pd;
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "/errors/business-exception", ex.getMessage(), req);
    }

    // 500
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "/errors/internal", "An unexpected error occurred", req);
    }

    // Builder
    private ProblemDetail problem(
            HttpStatus status,
            String type,
            String detail,
            HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(type));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }
}