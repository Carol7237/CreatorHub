package com.creatorhub.exception;

/**
 * Thrown when an entity cannot be found by its identifier (or other lookup key).
 * Intended HTTP mapping: <b>404 Not Found</b>.
 */
public class ResourceNotFoundException extends CreatorHubException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " not found with id: " + id);
    }
}
