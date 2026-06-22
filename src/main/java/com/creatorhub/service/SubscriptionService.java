package com.creatorhub.service;

import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.model.enums.SubStatus;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse create(SubscriptionRequest request);

    SubscriptionResponse findById(Long id);

    List<SubscriptionResponse> findAll();

    List<SubscriptionResponse> findByFan(Long fanId);

    List<SubscriptionResponse> findByTier(Long tierId);

    List<SubscriptionResponse> findByFanAndStatus(Long fanId, SubStatus status);

    /**
     * The meaningful "update" for a subscription: fan and tier are immutable, so
     * mutation happens through status transitions. Cancels an active subscription.
     */
    SubscriptionResponse cancel(Long id);

    void delete(Long id);
}
