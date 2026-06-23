package com.creatorhub.subscriptionservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.subscriptionservice.dto.SubscriptionResponse;
import com.creatorhub.subscriptionservice.dto.SubscriptionRequest;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse create(SubscriptionRequest request, Viewer viewer);

    List<SubscriptionResponse> findByFan(Long fanId);

    SubscriptionResponse cancel(Long id, Viewer viewer);

    void delete(Long id, Viewer viewer);

    /** Internal premium-gating contract: does the fan hold an ACTIVE subscription to the tier? */
    boolean hasActiveAccess(Long fanId, Long tierId);
}
