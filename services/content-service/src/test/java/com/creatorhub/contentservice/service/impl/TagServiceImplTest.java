package com.creatorhub.contentservice.service.impl;

import com.creatorhub.common.exception.BusinessRuleException;
import com.creatorhub.common.exception.DuplicateResourceException;
import com.creatorhub.common.exception.ResourceInUseException;
import com.creatorhub.contentservice.dto.TagRequest;
import com.creatorhub.contentservice.dto.TagResponse;
import com.creatorhub.contentservice.model.Post;
import com.creatorhub.contentservice.model.Tag;
import com.creatorhub.contentservice.repository.TagRepository;
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
    void create_new_ok() {
        when(tagRepository.existsByName("java")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });
        TagResponse resp = tagService.create(TagRequest.builder().name("  java  ").build());
        assertThat(resp.getName()).isEqualTo("java"); // trimmed
    }

    @Test
    void create_duplicate_throws() {
        when(tagRepository.existsByName("java")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,
                () -> tagService.create(TagRequest.builder().name("java").build()));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void create_blank_throws() {
        assertThrows(BusinessRuleException.class,
                () -> tagService.create(TagRequest.builder().name("   ").build()));
    }

    @Test
    void delete_usedByPosts_throwsInUse() {
        Tag t = new Tag();
        t.setId(1L);
        t.setName("java");
        t.getPosts().add(new Post());
        when(tagRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThrows(ResourceInUseException.class, () -> tagService.delete(1L));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void delete_clean_deletes() {
        Tag t = new Tag();
        t.setId(1L);
        t.setName("java");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(t));
        tagService.delete(1L);
        verify(tagRepository).delete(t);
    }
}
