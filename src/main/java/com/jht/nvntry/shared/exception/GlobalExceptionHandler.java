package com.jht.nvntry.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
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

    //--- 404
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "/errors/not-found", ex.getMessage(), request);
    }

    //--- 409
    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "/errors/conflict", ex.getMessage(), request);
    }

    /* 409 Idempotency
    * Returned when the same idempotency key is submitted more than once.
    * 200 vs 409 debate: we return 409 here to signal "this was a duplicate".
    * A 200 would be correct if we stored and returned the original response.
    * That is a Week 2 enhancement - for now, reject cleanly.
    * */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Check specifically idempotency constraint
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof PSQLException psql) {
            // SQL State 23505 = Unique Violation
            if ("23505".equals(psql.getSQLState())) {
                // Check if matches our specific constraint
                String constraint = psql.getServerErrorMessage() != null ?
                        psql.getServerErrorMessage().getConstraint() : null;

                if (constraint != null && constraint.contains("idempotency")) {
                    return problem(
                            HttpStatus.CONFLICT, "/errors/duplicate-movement", "Duplicate idempotency key", request
                    );
                }
            }
        }
        // Fallback for other DB constraints
        log.error("Unexpected date integrity violation", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "/errors/internal", "An unexpected error occurred", request);
    }

    /* 422
    * InsufficientStock is not a 400 (request was valid) nor a 409 (no resource conflict).
    * Is it a business rule violation on otherwise valid input - hence 422 Unprocessable.
    * */
    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/insufficient-stock", ex.getMessage(), request);
    }

    @ExceptionHandler(InactiveProductException.class)
    ProblemDetail handleInactiveProduct(InactiveProductException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/inactive-product", ex.getMessage(), request);
    }

    //--- 400 (Validation)
    // Handle @Valid on Body (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a
                ));
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation", "Validation failed", request);
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    // Handle @Validated on Class/Headers (ConstraintViolationException)
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleMissingRequestValue(ConstraintViolationException ex, HttpServletRequest request) {
        var fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a
                ));
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation", "Invalid request parameters", request);
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        var pd = problem(
                HttpStatus.BAD_REQUEST, "/errors/missing-header", "Require request header is missing", request
        );
        pd.setProperty("fieldErrors", Map.of(ex.getHeaderName(), "Header is required"));
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        var pd = problem(
                HttpStatus.BAD_REQUEST,
                "/errors/validation-failed",
                "Request body is malformed or contains invalid values",
                request
        );
        // Extract the root cause message
        Throwable cause = ex.getRootCause();
        String message = cause != null ? cause.getMessage() : ex.getMessage();

        // Parse common Jackson errors
        Map<String, String> fieldErrors = new HashMap<>();
        if (message != null) {
            if (message.contains("MovementType")) {
                fieldErrors.put("movementType", "Invalid movement type. Must be RECEIVE, SHIP, or ADJUST");
            }
            if (message.contains("UUID")) {
                fieldErrors.put("productId", "Invalid UUID format");
            }
        }

        if (!fieldErrors.isEmpty()) {
            pd.setProperty("fieldErrors", fieldErrors);
        } else {
            pd.setProperty("error", message);
        }
        return pd;
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "/errors/business-exception", ex.getMessage(), request);
    }

    //--- 500
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "/errors/internal", "An unexpected error occurred", request);
    }

    // Builder
    private ProblemDetail problem(
            HttpStatus status,
            String type,
            String detail,
            HttpServletRequest request
    ) {
        var pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(type));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }
}