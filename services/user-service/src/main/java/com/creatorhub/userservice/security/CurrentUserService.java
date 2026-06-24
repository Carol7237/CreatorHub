package com.creatorhub.userservice.security;

import com.creatorhub.common.Viewer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves the current request's {@link Viewer} from the {@code SecurityContext}
 * populated by {@link com.creatorhub.common.security.HeaderAuthenticationFilter} from
 * the gateway-injected identity headers. Controllers use this so owners come from the
 * authenticated context, never the request body.
 *
 * <p>Stateless model (Step 2): the principal is the user's id ({@code Long}) and ADMIN
 * is derived from the {@code ROLE_ADMIN} authority — identical to the downstream
 * services' {@code CurrentViewerService}.
 */
@Service
public class CurrentUserService {

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

    /** The current user; throws 403 if the request is anonymous (defensive — endpoints are already secured). */
    public Viewer requireViewer() {
        Viewer viewer = currentViewer();
        if (!viewer.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return viewer;
    }
}
