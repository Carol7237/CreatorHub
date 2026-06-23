package com.creatorhub.subscriptionservice.repository;

import com.creatorhub.subscriptionservice.model.Subscription;
import com.creatorhub.subscriptionservice.model.enums.SubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByFanId(Long fanId);

    List<Subscription> findByTierId(Long tierId);

    List<Subscription> findByFanIdAndStatus(Long fanId, SubStatus status);

    /** Used by the create rule AND by the internal access endpoint (premium gating). */
    boolean existsByFanIdAndTierIdAndStatus(Long fanId, Long tierId, SubStatus status);

    Page<Subscription> findByFanId(Long fanId, Pageable pageable);
}
