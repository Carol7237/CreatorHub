package com.creatorhub.contentservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A piece of content, free or premium. When {@code premium} is true the post is
 * only accessible to subscribers of the tier identified by {@code tierId}.
 *
 * <p>Microservices note: the author (User service) and the tier (Subscription
 * service) are referenced by id ({@code authorId}, {@code tierId}) — no
 * cross-service FK. Comments and Tags live in this same service.
 */
@Entity
@Getter
@Setter
@ToString
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean premium = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** The creator (User service) who authored this post — referenced by id, no FK. */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** Tier (Subscription service) required for access — referenced by id, no FK. Null for free posts. */
    @Column(name = "tier_id")
    private Long tierId;

    /** Comments belong to the post (composition): delete the post -> delete its comments. */
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();

    /**
     * Owning side of the many-to-many (holds the {@code post_tags} join table).
     * Cascade PERSIST/MERGE only — never REMOVE, so deleting a post must not
     * delete tags shared with other posts.
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Post other = (Post) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
