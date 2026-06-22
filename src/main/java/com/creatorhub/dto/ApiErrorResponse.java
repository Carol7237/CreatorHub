package com.creatorhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Uniform JSON error body for the API. Used now by the security handlers
 * (401/403) and the login endpoint; the global @RestControllerAdvice in the
 * Views phase will reuse the same shape for 404/409/400.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
