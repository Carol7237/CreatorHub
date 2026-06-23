package com.creatorhub.security;

import com.creatorhub.common.Viewer;
import com.creatorhub.model.User;
import com.creatorhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the current request's user from the security context. Controllers use
 * this to obtain a {@link Viewer} which they pass into services (owners therefore
 * come from the authenticated context, never the request body).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /** The current user, or {@link Viewer#anonymous()} for unauthenticated requests. */
    @Transactional(readOnly = true)
    public Viewer currentViewer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Viewer.anonymous();
        }
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        Long userId = userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
        return new Viewer(userId, admin);
    }

    /** The current user; throws 403 if the request is anonymous (defensive — endpoints are already secured). */
    @Transactional(readOnly = true)
    public Viewer requireViewer() {
        Viewer viewer = currentViewer();
        if (!viewer.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return viewer;
    }
}
