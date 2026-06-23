package com.creatorhub.service.impl;

import com.creatorhub.dto.TagRequest;
import com.creatorhub.dto.TagResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.exception.ResourceInUseException;
import com.creatorhub.exception.ResourceNotFoundException;
import com.creatorhub.model.Post;
import com.creatorhub.model.Tag;
import com.creatorhub.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock private TagRepository tagRepository;

    @InjectMocks private TagServiceImpl tagService;

    @Test
    void create_valid_succeeds() {
        when(tagRepository.existsByName("news")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(3L);
            return t;
        });

        TagResponse response = tagService.create(TagRequest.builder().name("news").build());

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("news");
    }

    @Test
    void create_duplicate_throws() {
        when(tagRepository.existsByName("news")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> tagService.create(TagRequest.builder().name("news").build()));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void create_blankName_throws() {
        assertThrows(BusinessRuleException.class,
                () -> tagService.create(TagRequest.builder().name("   ").build()));
    }

    @Test
    void findByName_notFound_throws() {
        when(tagRepository.findByName("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> tagService.findByName("ghost"));
    }

    @Test
    void update_toExistingName_throws() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.setName("old");
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("taken")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> tagService.update(3L, TagRequest.builder().name("taken").build()));
    }

    @Test
    void delete_withPosts_throwsInUse() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.getPosts().add(new Post());
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));
        assertThrows(ResourceInUseException.class, () -> tagService.delete(3L));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void delete_clean_deletes() {
        Tag tag = new Tag();
        tag.setId(3L);
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));
        tagService.delete(3L);
        verify(tagRepository).delete(tag);
    }
}
