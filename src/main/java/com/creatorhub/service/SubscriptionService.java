package com.creatorhub.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.model.enums.SubStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse create(SubscriptionRequest request, Viewer viewer);

    SubscriptionResponse findById(Long id);

    List<SubscriptionResponse> findAll();

    PagedResponse<SubscriptionResponse> findAll(Pageable pageable);

    List<SubscriptionResponse> findByFan(Long fanId);

    PagedResponse<SubscriptionResponse> findByFan(Long fanId, Pageable pageable);

    List<SubscriptionResponse> findByTier(Long tierId);

    List<SubscriptionResponse> findByFanAndStatus(Long fanId, SubStatus status);

    /** Cancels a subscription (owner or admin only). */
    SubscriptionResponse cancel(Long id, Viewer viewer);

    void delete(Long id, Viewer viewer);
}
