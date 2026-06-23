package com.creatorhub.userservice.security;

import com.creatorhub.userservice.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter that exposes a domain {@link User} to Spring Security as a
 * {@link UserDetails}, WITHOUT making the entity implement framework interfaces
 * (keeps the domain model decoupled from Spring Security).
 *
 * <p>The single {@code role} is mapped to a {@code ROLE_<name>} authority, which
 * is what {@code hasRole(...)} expects.
 */
public class SecurityUser implements UserDetails {

    private final transient User user;

    public SecurityUser(User user) {
        this.user = user;
    }

    /** Convenience accessor for the underlying account id (used by controllers). */
    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
