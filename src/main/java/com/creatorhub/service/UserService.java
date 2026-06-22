package com.creatorhub.service;

import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse findById(Long id);

    UserResponse findByUsername(String username);

    List<UserResponse> findAll();

    UserResponse update(Long id, UserRequest request);

    void delete(Long id);
}
