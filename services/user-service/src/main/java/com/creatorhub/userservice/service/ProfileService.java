package com.creatorhub.userservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.userservice.dto.ProfileRequest;
import com.creatorhub.userservice.dto.ProfileResponse;

import java.util.List;

/**
 * Profile is a dependent of {@link UserService}: it is created automatically with
 * the user and removed (cascade) with it. Hence read + update only (no standalone
 * create/delete). Update is restricted to the profile's owner (or an admin).
 */
public interface ProfileService {

    ProfileResponse findById(Long id);

    ProfileResponse findByUserId(Long userId);

    List<ProfileResponse> findAll();

    ProfileResponse update(Long id, ProfileRequest request, Viewer viewer);
}
