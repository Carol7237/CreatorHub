package com.creatorhub.model;

import com.creatorhub.model.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;

/**
 * Base account. A single user can act as both creator AND fan with the same
 * account. Table is named {@code users} because {@code user} is a reserved word
 * in PostgreSQL.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** Will hold a BCrypt hash from the Security phase onward; a plain field for now. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    // --- Relations ---

    /** Inverse side of the 1:1; Profile owns the FK. Composition: delete the user -> delete the profile. */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Profile profile;

    /** As a creator: the tiers this user offers. */
    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<SubscriptionTier> tiers = new ArrayList<>();

    /** As a creator: the posts this user publishes. */
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();

    /** Comments this user has written. */
    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();

    /** As a fan: the subscriptions this user holds. */
    @OneToMany(mappedBy = "fan", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Subscription> subscriptions = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User other = (User) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
