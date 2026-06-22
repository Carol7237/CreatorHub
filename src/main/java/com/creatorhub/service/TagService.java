package com.creatorhub.service;

import com.creatorhub.dto.TagRequest;
import com.creatorhub.dto.TagResponse;

import java.util.List;

public interface TagService {

    TagResponse create(TagRequest request);

    TagResponse findById(Long id);

    TagResponse findByName(String name);

    List<TagResponse> findAll();

    TagResponse update(Long id, TagRequest request);

    void delete(Long id);
}
