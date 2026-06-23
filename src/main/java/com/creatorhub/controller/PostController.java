package com.creatorhub.controller;

import com.creatorhub.dto.CommentResponse;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.security.CurrentUserService;
import com.creatorhub.service.CommentService;
import com.creatorhub.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final CurrentUserService currentUserService;

    /** Public: browse posts (premium bodies are locked for viewers without access). */
    @GetMapping
    public PagedResponse<PostResponse> list(Pageable pageable) {
        return postService.findAll(pageable, currentUserService.currentViewer());
    }

    /** Public: a single post (premium body locked unless the viewer has access). */
    @GetMapping("/{id}")
    public PostResponse getById(@PathVariable Long id) {
        return postService.findById(id, currentUserService.currentViewer());
    }

    /** Public: comments on a post. */
    @GetMapping("/{postId}/comments")
    public List<CommentResponse> comments(@PathVariable Long postId) {
        return commentService.findByPost(postId);
    }

    /** Authenticated: create a post (author = current user). */
    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) {
        PostResponse created = postService.create(request, currentUserService.requireViewer());
        return ResponseEntity.created(URI.create("/api/posts/" + created.getId())).body(created);
    }

    /** Authenticated (owner/admin): update a post. */
    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request, currentUserService.requireViewer());
    }

    /** Authenticated (owner/admin): delete a post. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id, currentUserService.requireViewer());
        return ResponseEntity.noContent().build();
    }
}
