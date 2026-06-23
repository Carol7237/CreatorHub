package com.creatorhub.service.impl;

import com.creatorhub.dto.ProfileRequest;
import com.creatorhub.dto.ProfileResponse;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Profile;
import com.creatorhub.model.User;
import com.creatorhub.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private ProfileRepository profileRepository;

    @InjectMocks private ProfileServiceImpl profileService;

    private static Profile profile(Long id) {
        User u = new User();
        u.setId(id);
        Profile p = new Profile();
        p.setId(id);
        p.setDisplayName("Name " + id);
        p.setUser(u);
        return p;
    }

    @Test
    void findById_found_returnsResponse() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile(1L)));
        assertThat(profileService.findById(1L).getDisplayName()).isEqualTo("Name 1");
    }

    @Test
    void findById_notFound_throws() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> profileService.findById(99L));
    }

    @Test
    void findByUserId_notFound_throws() {
        when(profileRepository.findByUserId(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> profileService.findByUserId(5L));
    }

    @Test
    void findAll_mapsEntities() {
        when(profileRepository.findAll()).thenReturn(List.of(profile(1L), profile(2L)));
        assertThat(profileService.findAll()).hasSize(2);
    }

    @Test
    void update_changesFields() {
        Profile p = profile(1L);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(p));

        ProfileResponse response = profileService.update(1L,
                ProfileRequest.builder().displayName("Updated").bio("hello").build());

        assertThat(response.getDisplayName()).isEqualTo("Updated");
        assertThat(p.getDisplayName()).isEqualTo("Updated");
        assertThat(p.getBio()).isEqualTo("hello");
    }
}
