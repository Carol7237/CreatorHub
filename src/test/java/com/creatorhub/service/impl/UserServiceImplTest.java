package com.creatorhub.service.impl;

import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.Profile;
import com.creatorhub.model.User;
import com.creatorhub.model.enums.Role;
import com.creatorhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests (no Spring context, no DB) for {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private static User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword("ENCODED");
        u.setRole(Role.USER);
        Profile p = new Profile();
        p.setId(id);
        p.setDisplayName(username);
        p.setUser(u);
        u.setProfile(p);
        return u;
    }

    private static UserRequest request(String username) {
        return UserRequest.builder()
                .username(username).email(username + "@example.com")
                .password("rawpass").displayName("Display " + username).build();
    }

    @Test
    void create_success_encodesPasswordAndCreatesProfile() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawpass")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.create(request("alice"));

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRole()).isEqualTo(Role.USER);

        verify(passwordEncoder).encode("rawpass");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("HASHED");           // never the raw password
        assertThat(saved.getProfile()).isNotNull();                     // profile auto-created
        assertThat(saved.getProfile().getDisplayName()).isEqualTo("Display alice");
        assertThat(saved.getProfile().getUser()).isSameAs(saved);       // both sides wired
    }

    @Test
    void create_duplicateUsername_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.create(request("alice")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.create(request("alice")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        assertThat(userService.findById(1L).getUsername()).isEqualTo("alice");
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
    }

    @Test
    void findByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.findByUsername("ghost"));
    }

    @Test
    void findAll_mapsEntities() {
        when(userRepository.findAll()).thenReturn(List.of(user(1L, "a"), user(2L, "b")));
        assertThat(userService.findAll()).hasSize(2);
    }

    @Test
    void findAll_paged_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, "a")), pageable, 1));
        var page = userService.findAll(pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void update_changeEmail_succeeds() {
        User existing = user(1L, "alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        UserResponse response = userService.update(1L,
                UserRequest.builder().email("new@example.com").build());

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void update_duplicateEmail_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> userService.update(1L,
                UserRequest.builder().email("taken@example.com").build()));
    }

    @Test
    void delete_withDependencies_throwsInUse() {
        User u = user(1L, "alice");
        u.getPosts().add(new Post()); // has a dependent post
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThrows(ResourceInUseException.class, () -> userService.delete(1L));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_clean_deletes() {
        User u = user(1L, "alice"); // no tiers/posts/comments/subscriptions
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        userService.delete(1L);
        verify(userRepository).delete(u);
    }
}
