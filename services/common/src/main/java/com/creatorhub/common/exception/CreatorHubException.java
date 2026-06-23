package com.creatorhub.common.exception;

/**
 * Base type for all domain exceptions thrown by the service layers across the
 * CreatorHub microservices. A single root lets each service's global
 * {@code @RestControllerAdvice} provide consistent fallback handling.
 */
public abstract class CreatorHubException extends RuntimeException {

    protected CreatorHubException(String message) {
        super(message);
    }
}
