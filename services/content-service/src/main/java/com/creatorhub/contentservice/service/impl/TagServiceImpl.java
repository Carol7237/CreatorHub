package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.DuplicateResourceException;
import com.creatorhub.common.exception.ResourceInUseException;
import com.creatorhub.common.exception.ResourceNotFoundException;
import com.creatorhub.contentservice.dto.TagRequest;
import com.creatorhub.contentservice.dto.TagResponse;
import com.creatorhub.contentservice.dto.mapper.TagMapper;
import com.creatorhub.contentservice.model.Tag;
import com.creatorhub.contentservice.repository.TagRepository;
import com.creatorhub.contentservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @Override
    public TagResponse create(TagRequest request) {
        String name = normalize(request.getName());
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag", "name", name);
        }
        Tag tag = new Tag();
        tag.setName(name);
        return TagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse findById(Long id) {
        return TagMapper.toResponse(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> findAll() {
        return tagRepository.findAll().stream().map(TagMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Tag tag = getOrThrow(id);
        if (!tag.getPosts().isEmpty()) {
            throw new ResourceInUseException("Cannot delete tag " + id + ": it is still used by posts.");
        }
        tagRepository.delete(tag);
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Tag name must not be blank");
        }
        return name.trim();
    }

    private Tag getOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
    }
}
