package com.creatorhub.contentservice.service;

import com.creatorhub.contentservice.dto.TagRequest;
import com.creatorhub.contentservice.dto.TagResponse;

import java.util.List;

public interface TagService {

    TagResponse create(TagRequest request);

    TagResponse findById(Long id);

    List<TagResponse> findAll();

    void delete(Long id);
}
