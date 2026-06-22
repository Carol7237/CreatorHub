package com.creatorhub.repository;

import com.creatorhub.model.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Long> {

    List<SubscriptionTier> findByCreatorId(Long creatorId);
}
