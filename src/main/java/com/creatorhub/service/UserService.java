package com.creatorhub.service;

import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse findById(Long id);

    UserResponse findByUsername(String username);

    List<UserResponse> findAll();

    /** Paginated + sorted (allowed sort: id, username, email, role, enabled). */
    PagedResponse<UserResponse> findAll(Pageable pageable);

    UserResponse update(Long id, UserRequest request);

    void delete(Long id);
}
