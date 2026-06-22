package com.creatorhub.service;

import com.creatorhub.dto.ProfileRequest;
import com.creatorhub.dto.ProfileResponse;

import java.util.List;

/**
 * Profile is a dependent of {@link UserService}: it is created automatically
 * when a user is created and removed (cascade) when the user is deleted.
 * Therefore this service intentionally exposes only read + update, not
 * standalone create/delete (a profile cannot exist without its user).
 */
public interface ProfileService {

    ProfileResponse findById(Long id);

    ProfileResponse findByUserId(Long userId);

    List<ProfileResponse> findAll();

    ProfileResponse update(Long id, ProfileRequest request);
}
