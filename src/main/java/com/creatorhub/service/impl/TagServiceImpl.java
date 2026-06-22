package com.creatorhub.service.impl;

import com.creatorhub.dto.TagRequest;
import com.creatorhub.dto.TagResponse;
import com.creatorhub.dto.mapper.TagMapper;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Tag;
import com.creatorhub.repository.TagRepository;
import com.creatorhub.service.TagService;
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
    public TagResponse findByName(String name) {
        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with name: " + name));
        return TagMapper.toResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> findAll() {
        return tagRepository.findAll().stream().map(TagMapper::toResponse).toList();
    }

    @Override
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = getOrThrow(id);
        String name = normalize(request.getName());
        if (!name.equals(tag.getName())) {
            if (tagRepository.existsByName(name)) {
                throw new DuplicateResourceException("Tag", "name", name);
            }
            tag.setName(name);
        }
        return TagMapper.toResponse(tag);
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
