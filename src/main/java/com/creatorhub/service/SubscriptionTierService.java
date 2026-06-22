package com.creatorhub.service;

import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.SubscriptionTierResponse;

import java.util.List;

public interface SubscriptionTierService {

    SubscriptionTierResponse create(SubscriptionTierRequest request);

    SubscriptionTierResponse findById(Long id);

    List<SubscriptionTierResponse> findAll();

    List<SubscriptionTierResponse> findByCreator(Long creatorId);

    SubscriptionTierResponse update(Long id, SubscriptionTierRequest request);

    void delete(Long id);
}
