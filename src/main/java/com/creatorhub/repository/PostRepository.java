package com.creatorhub.repository;

import com.creatorhub.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // NOTE: the Post entity's creator field is named "author", so the derived
    // queries use "AuthorId" (the project domain still calls this the creator).
    List<Post> findByAuthorId(Long authorId);

    List<Post> findByPremium(boolean premium);

    List<Post> findByAuthorIdAndPremium(Long authorId, boolean premium);

    // --- Paginated variants (Phase 5) ---
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    Page<Post> findByPremium(boolean premium, Pageable pageable);
}
