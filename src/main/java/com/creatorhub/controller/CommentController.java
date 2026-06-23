package com.creatorhub.controller;

import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.security.CurrentUserService;
import com.creatorhub.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CurrentUserService currentUserService;

    /** Authenticated: comment on a post (author = current user; premium posts require access). */
    @PostMapping
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentRequest request) {
        CommentResponse created = commentService.create(request, currentUserService.requireViewer());
        return ResponseEntity.created(URI.create("/api/comments/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return commentService.update(id, request, currentUserService.requireViewer());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id, currentUserService.requireViewer());
        return ResponseEntity.noContent().build();
    }
}
