package com.creatorhub.contentservice.repository;

import com.creatorhub.contentservice.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
}
