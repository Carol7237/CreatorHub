package com.creatorhub.common;

/**
 * The user acting on / viewing a request, resolved from the security context by
 * the controllers and passed explicitly into the service layer. Keeping this a
 * plain value (instead of reading {@code SecurityContextHolder} inside services)
 * keeps the domain services decoupled from Spring Security and trivially unit
 * testable.
 *
 * @param userId the authenticated user's id, or {@code null} for an anonymous request
 * @param admin  whether the user has the ADMIN role
 */
public record Viewer(Long userId, boolean admin) {

    public static Viewer anonymous() {
        return new Viewer(null, false);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    /** True if this viewer is the given owner, or an admin. */
    public boolean isOwnerOrAdmin(Long ownerId) {
        return admin || (userId != null && userId.equals(ownerId));
    }
}
