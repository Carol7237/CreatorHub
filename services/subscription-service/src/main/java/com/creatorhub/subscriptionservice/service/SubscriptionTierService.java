package com.creatorhub.subscriptionservice.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierRequest;
import com.creatorhub.subscriptionservice.dto.SubscriptionTierResponse;

import java.util.List;

public interface SubscriptionTierService {

    SubscriptionTierResponse create(SubscriptionTierRequest request, Viewer viewer);

    SubscriptionTierResponse findById(Long id);

    List<SubscriptionTierResponse> findAll();

    List<SubscriptionTierResponse> findByCreator(Long creatorId);

    SubscriptionTierResponse update(Long id, SubscriptionTierRequest request, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
