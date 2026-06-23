package com.creatorhub.subscriptionservice.repository;

import com.creatorhub.subscriptionservice.model.SubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTierRepository extends JpaRepository<SubscriptionTier, Long> {

    List<SubscriptionTier> findByCreatorId(Long creatorId);
}
