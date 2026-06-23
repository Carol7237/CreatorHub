package com.creatorhub.contentservice.controller;

import com.creatorhub.contentservice.dto.TagRequest;
import com.creatorhub.contentservice.dto.TagResponse;
import com.creatorhub.contentservice.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public List<TagResponse> list() {
        return tagService.findAll();
    }

    @GetMapping("/{id}")
    public TagResponse getById(@PathVariable Long id) {
        return tagService.findById(id);
    }

    /** Authenticated: create a tag (tags are shared across posts). */
    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        TagResponse created = tagService.create(request);
        return ResponseEntity.created(URI.create("/api/tags/" + created.getId())).body(created);
    }

    /** Admin only: deleting a shared tag is restricted. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
