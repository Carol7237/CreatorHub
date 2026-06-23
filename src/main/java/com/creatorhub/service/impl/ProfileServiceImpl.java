package com.creatorhub.service.impl;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.ProfileRequest;
import com.creatorhub.dto.ProfileResponse;
import com.creatorhub.dto.mapper.ProfileMapper;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Profile;
import com.creatorhub.repository.ProfileRepository;
import com.creatorhub.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse findById(Long id) {
        return ProfileMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse findByUserId(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
        return ProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> findAll() {
        return profileRepository.findAll().stream().map(ProfileMapper::toResponse).toList();
    }

    @Override
    public ProfileResponse update(Long id, ProfileRequest request, Viewer viewer) {
        Profile profile = getOrThrow(id);
        Long ownerId = profile.getUser() != null ? profile.getUser().getId() : null;
        if (!viewer.isOwnerOrAdmin(ownerId)) {
            throw new AccessDeniedException("You can only edit your own profile");
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        return ProfileMapper.toResponse(profile);
    }

    private Profile getOrThrow(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", id));
    }
}
