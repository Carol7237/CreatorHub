package com.creatorhub.exception;

import com.creatorhub.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception -> JSON mapping for the API, per CLAUDE.md §9, using the
 * shared {@link ApiErrorResponse}. Client/domain errors are logged at WARN; truly
 * unexpected exceptions at ERROR (dedicated error log). Bean Validation failures
 * become 400 with per-field messages.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** @Valid failures on a @RequestBody -> 400 with a field -> message map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(),
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        }
        log.warn("400 {} {} -> validation failed: {}", request.getMethod(), request.getRequestURI(), fieldErrors);
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    /** Malformed/unreadable JSON body -> 400 (resolves the Phase 5 limitation of returning 500). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request) {
        log.warn("400 {} {} -> malformed request body", request.getMethod(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("404 {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({DuplicateResourceException.class, ResourceInUseException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(CreatorHubException ex, HttpServletRequest request) {
        log.warn("409 {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("400 {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Method-security (@PreAuthorize) denials reaching the controller layer. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("403 {} {} -> {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request);
    }

    /** Last-resort handler: unexpected errors are logged at ERROR (dedicated error log). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("500 {} {} -> unexpected error", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
