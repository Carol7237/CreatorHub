package com.creatorhub.common.security;

import com.creatorhub.common.Viewer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the current {@link Viewer} from the {@code SecurityContext} populated
 * by {@link HeaderAuthenticationFilter}. For downstream (stateless, header-auth)
 * services: the principal is the user's id ({@code Long}), and ADMIN is derived
 * from the {@code ROLE_ADMIN} authority.
 */
public class CurrentViewerService {

    /** The current user, or {@link Viewer#anonymous()} for unauthenticated requests. */
    public Viewer currentViewer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Viewer.anonymous();
        }
        if (!(auth.getPrincipal() instanceof Long userId)) {
            return Viewer.anonymous();
        }
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Viewer(userId, admin);
    }

    /** The current user; throws 403 if anonymous (defensive — endpoints are already secured). */
    public Viewer requireViewer() {
        Viewer viewer = currentViewer();
        if (!viewer.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return viewer;
    }
}
