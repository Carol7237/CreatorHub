package com.creatorhub.common.exception;

/**
 * Thrown when a domain/business rule is violated by the request data (e.g. a
 * premium post without a tier, a free post with a tier, self-subscription, a
 * non-positive tier price, or an invalid sort property).
 * Intended HTTP mapping: <b>400 Bad Request</b>.
 */
public class BusinessRuleException extends CreatorHubException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
