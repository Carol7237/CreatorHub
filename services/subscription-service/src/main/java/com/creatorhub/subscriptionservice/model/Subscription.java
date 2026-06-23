package com.creatorhub.subscriptionservice.model;

import com.creatorhub.subscriptionservice.model.enums.SubStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.time.LocalDate;

/**
 * A fan's subscription to a creator's tier. Link entity carrying its own data
 * (start date and status).
 *
 * <p>Microservices note: the fan is a user owned by the User service → referenced
 * by {@code fanId} (no cross-service FK). The tier is in this same service, so the
 * relation to {@link SubscriptionTier} stays a normal JPA association.
 */
@Entity
@Getter
@Setter
@ToString
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The fan (User service) who subscribes — referenced by id, no FK. */
    @Column(name = "fan_id", nullable = false)
    private Long fanId;

    @Column(nullable = false, updatable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubStatus status = SubStatus.ACTIVE;

    /** The tier being subscribed to (same service/schema → normal JPA relation). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    @ToString.Exclude
    private SubscriptionTier tier;

    @PrePersist
    void onCreate() {
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Subscription other = (Subscription) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
