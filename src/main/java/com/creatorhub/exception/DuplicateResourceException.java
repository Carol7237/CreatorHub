package com.creatorhub.exception;

/**
 * Thrown when a uniqueness constraint would be violated (e.g. username/email/tag
 * name already taken, or a duplicate active subscription).
 * Intended HTTP mapping: <b>409 Conflict</b>.
 */
public class DuplicateResourceException extends CreatorHubException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(resourceName + " already exists with " + field + ": " + value);
    }
}
