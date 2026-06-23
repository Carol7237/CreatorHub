package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform JSON error body for the API. Used by the security handlers (401/403),
 * the login endpoint, and the global {@code @RestControllerAdvice} (404/409/400).
 * For validation failures, {@link #fieldErrors} maps each invalid field to its
 * message; it is omitted from the JSON otherwise.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** field name -> validation message; null (omitted) for non-validation errors. */
    private Map<String, String> fieldErrors;
}
