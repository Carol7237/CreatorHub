package com.creatorhub.service.impl;

import com.creatorhub.common.PageableUtils;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.dto.mapper.UserMapper;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Profile;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.Role;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    /** Whitelisted sort properties for users — NOTE: never includes "password". */
    private static final Set<String> ALLOWED_SORT = Set.of("id", "username", "email", "role", "enabled");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // Store a BCrypt hash, never the plaintext password.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        // Business rule: every user gets a profile automatically (composition).
        Profile profile = new Profile();
        profile.setDisplayName(request.getDisplayName() != null && !request.getDisplayName().isBlank()
                ? request.getDisplayName()
                : request.getUsername());
        profile.setBio(request.getBio());
        profile.setAvatarUrl(request.getAvatarUrl());

        // Wire both sides; cascade ALL on User.profile persists the profile.
        profile.setUser(user);
        user.setProfile(profile);

        User saved = userRepository.save(user);
        // SECURITY: log identifiers only, never the password.
        log.info("User created: id={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return UserMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(Pageable pageable) {
        Pageable safe = PageableUtils.sanitize(pageable, ALLOWED_SORT);
        log.debug("findAll users page={} size={} sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        return PagedResponse.from(userRepository.findAll(safe).map(UserMapper::toResponse));
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {
        User user = getOrThrow(id);

        // Uniqueness is only re-checked when the value actually changes.
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("User", "username", request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        // Keep the profile in sync with any profile fields supplied.
        Profile profile = user.getProfile();
        if (profile != null) {
            if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                profile.setDisplayName(request.getDisplayName());
            }
            if (request.getBio() != null) {
                profile.setBio(request.getBio());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
        }

        return UserMapper.toResponse(user);
    }

    @Override
    public void delete(Long id) {
        User user = getOrThrow(id);

        // Safe delete: refuse if the user still owns content/relationships that
        // are not cascade-managed (only the profile cascades away). The caller
        // must clean those up first. (Decision documented in CLAUDE.md.)
        if (!user.getTiers().isEmpty() || !user.getPosts().isEmpty()
                || !user.getComments().isEmpty() || !user.getSubscriptions().isEmpty()) {
            log.warn("Blocked delete of user id={}: still has dependent tiers/posts/comments/subscriptions", id);
            throw new ResourceInUseException("Cannot delete user " + id
                    + ": they still have tiers, posts, comments or subscriptions. Remove those first.");
        }

        userRepository.delete(user);
        log.info("User deleted: id={}", id);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
