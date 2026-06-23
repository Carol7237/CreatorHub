package com.creatorhub.common.exception;

/**
 * Thrown when an entity cannot be deleted because other records still depend on
 * it (e.g. deleting a creator who still has posts/tiers, or a tag still used by
 * posts). Intended HTTP mapping: <b>409 Conflict</b>.
 */
public class ResourceInUseException extends CreatorHubException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
