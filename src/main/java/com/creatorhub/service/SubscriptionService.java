package com.creatorhub.service;

import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.model.enums.SubStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse create(SubscriptionRequest request);

    SubscriptionResponse findById(Long id);

    List<SubscriptionResponse> findAll();

    /** Paginated + sorted (allowed sort: id, startDate, status). */
    PagedResponse<SubscriptionResponse> findAll(Pageable pageable);

    List<SubscriptionResponse> findByFan(Long fanId);

    /** Paginated + sorted subscriptions of a fan. */
    PagedResponse<SubscriptionResponse> findByFan(Long fanId, Pageable pageable);

    List<SubscriptionResponse> findByTier(Long tierId);

    List<SubscriptionResponse> findByFanAndStatus(Long fanId, SubStatus status);

    /**
     * The meaningful "update" for a subscription: fan and tier are immutable, so
     * mutation happens through status transitions. Cancels an active subscription.
     */
    SubscriptionResponse cancel(Long id);

    void delete(Long id);
}
