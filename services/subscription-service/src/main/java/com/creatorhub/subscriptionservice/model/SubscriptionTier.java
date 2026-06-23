package com.creatorhub.subscriptionservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 *
 * <p>Microservices note: the creator is a user owned by the User service, so it is
 * referenced by {@code creatorId} (a {@code Long}) — no cross-service FK. The Post
 * side of the old relationship lives in the Content service and references this
 * tier by id.
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

    /** The creator (User service) offering this tier — referenced by id, no FK. */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /** Subscriptions placed on this tier (same service/schema → normal JPA relation). */
    @OneToMany(mappedBy = "tier", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Subscription> subscriptions = new ArrayList<>();

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
