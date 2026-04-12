package com.jht.nvntry.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;
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
    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "/errors/duplicate-movement", ex.getMessage(), req);
    }

    /* 422
    * InsufficientStock os not a 400 (request was valid) nor a 409 (no resource conflict).
    * Is it a business rule violation on otherwise valid input - hence 422 Unprocessable.
    * */
    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "/errors/insufficient-stock", ex.getMessage(), req);
    }

    // 400 (Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fieldErros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a // keep first message per field
                ));
        var pd = problem(HttpStatus.BAD_REQUEST, "/errors/validation-failed", "Request validation faild", req);
        pd.setProperty("fieldErrors", fieldErros);
        return pd;
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