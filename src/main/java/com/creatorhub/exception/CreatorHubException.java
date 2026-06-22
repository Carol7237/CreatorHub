package com.creatorhub.exception;

/**
 * Base type for all domain exceptions thrown by the service layer. Having a
 * single root makes it easy for the global {@code @RestControllerAdvice}
 * (added in the Views phase) to provide a fallback handler.
 */
public abstract class CreatorHubException extends RuntimeException {

    protected CreatorHubException(String message) {
        super(message);
    }
}
