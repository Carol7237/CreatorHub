package com.creatorhub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A subscription level offered by a creator (e.g. "Basic", "VIP").
 */
@Entity
@Getter
@Setter
@ToString
public class SubscriptionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Money is always BigDecimal, never double. numeric(10,2). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    @Column(length = 2000)
    private String perks;

    // --- Relations ---

    /** The creator offering this tier. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @ToString.Exclude
    private User creator;

    /** Subscriptions placed on this tier. */
    @OneToMany(mappedBy = "tier", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Subscription> subscriptions = new ArrayList<>();

    /** Posts gated behind this tier. */
    @OneToMany(mappedBy = "tier", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Post> posts = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SubscriptionTier other = (SubscriptionTier) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
