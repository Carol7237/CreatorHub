package com.creatorhub.repository;

import com.creatorhub.model.Subscription;
import com.creatorhub.model.enums.SubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByFanId(Long fanId);

    List<Subscription> findByTierId(Long tierId);

    List<Subscription> findByFanIdAndStatus(Long fanId, SubStatus status);

    /** Used to prevent a fan from subscribing twice (actively) to the same tier. */
    boolean existsByFanIdAndTierIdAndStatus(Long fanId, Long tierId, SubStatus status);

    // --- Paginated variants (Phase 5) ---
    Page<Subscription> findByFanId(Long fanId, Pageable pageable);
}
