package com.creatorhub.service;

import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.SubscriptionTierResponse;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.exception.DuplicateResourceException;
import com.creatorhub.model.enums.SubStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 2 smoke / flow tests for the service layer. They run against the Docker
 * PostgreSQL (localhost:5433) and roll back after each test (@Transactional), so
 * they are repeatable. A dedicated test profile (H2 / Testcontainers) will be
 * introduced in the Testing phase.
 */
@SpringBootTest
@Transactional
class Phase2ServiceFlowTests {

    @Autowired
    private UserService userService;
    @Autowired
    private SubscriptionTierService tierService;
    @Autowired
    private PostService postService;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private CommentService commentService;

    // --- helpers ---

    private UserResponse newUser(String username) {
        return userService.create(UserRequest.builder()
                .username(username)
                .email(username + "@example.com")
                .password("secret")
                .build());
    }

    private SubscriptionTierResponse newTier(Long creatorId, String name, String price) {
        return tierService.create(SubscriptionTierRequest.builder()
                .name(name)
                .priceMonthly(new BigDecimal(price))
                .creatorId(creatorId)
                .build());
    }

    // --- tests ---

    @Test
    @DisplayName("Creating a user auto-creates its profile (displayName defaults to username)")
    void createUser_autoCreatesProfile() {
        UserResponse user = newUser("creator_profile");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getProfileId()).isNotNull();
        assertThat(user.getDisplayName()).isEqualTo("creator_profile");
        assertThat(user.getEmail()).isEqualTo("creator_profile@example.com");
    }

    @Test
    @DisplayName("Happy flow: creator -> tier -> premium post -> fan subscribes -> comment")
    void fullHappyFlow() {
        UserResponse creator = newUser("creator_flow");
        SubscriptionTierResponse tier = newTier(creator.getId(), "VIP", "9.99");

        PostResponse post = postService.create(PostRequest.builder()
                .title("Behind the scenes")
                .body("Premium content body")
                .premium(true)
                .authorId(creator.getId())
                .tierId(tier.getId())
                .tags(Set.of("news", "update"))
                .build());

        assertThat(post.isPremium()).isTrue();
        assertThat(post.getTierId()).isEqualTo(tier.getId());
        assertThat(post.getCreatorId()).isEqualTo(creator.getId());
        assertThat(post.getTags()).containsExactlyInAnyOrder("news", "update");

        UserResponse fan = newUser("fan_flow");
        SubscriptionResponse sub = subscriptionService.create(SubscriptionRequest.builder()
                .fanId(fan.getId())
                .tierId(tier.getId())
                .build());

        assertThat(sub.getStatus()).isEqualTo(SubStatus.ACTIVE);
        assertThat(sub.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(sub.getCreatorId()).isEqualTo(creator.getId());

        CommentResponse comment = commentService.create(CommentRequest.builder()
                .text("Loved this!")
                .postId(post.getId())
                .authorId(fan.getId())
                .build());

        assertThat(comment.getId()).isNotNull();
        assertThat(commentService.findByPost(post.getId())).hasSize(1);
        assertThat(subscriptionService.findByFan(fan.getId())).hasSize(1);
        assertThat(postService.findById(post.getId()).getTitle()).isEqualTo("Behind the scenes");
    }

    @Test
    @DisplayName("A second active subscription to the same tier is rejected")
    void duplicateActiveSubscription_throws() {
        UserResponse creator = newUser("creator_dupsub");
        SubscriptionTierResponse tier = newTier(creator.getId(), "Basic", "5.00");
        UserResponse fan = newUser("fan_dupsub");

        subscriptionService.create(SubscriptionRequest.builder()
                .fanId(fan.getId()).tierId(tier.getId()).build());

        assertThrows(DuplicateResourceException.class, () ->
                subscriptionService.create(SubscriptionRequest.builder()
                        .fanId(fan.getId()).tierId(tier.getId()).build()));
    }

    @Test
    @DisplayName("A creator cannot subscribe to their own tier")
    void selfSubscription_throws() {
        UserResponse creator = newUser("creator_self");
        SubscriptionTierResponse tier = newTier(creator.getId(), "Basic", "5.00");

        assertThrows(BusinessRuleException.class, () ->
                subscriptionService.create(SubscriptionRequest.builder()
                        .fanId(creator.getId()).tierId(tier.getId()).build()));
    }

    @Test
    @DisplayName("A premium post without a tier is rejected")
    void premiumPostWithoutTier_throws() {
        UserResponse creator = newUser("creator_premnotier");

        assertThrows(BusinessRuleException.class, () ->
                postService.create(PostRequest.builder()
                        .title("t").body("b").premium(true)
                        .authorId(creator.getId()).tierId(null).build()));
    }

    @Test
    @DisplayName("A free post with a tier is rejected")
    void freePostWithTier_throws() {
        UserResponse creator = newUser("creator_freetier");
        SubscriptionTierResponse tier = newTier(creator.getId(), "Basic", "5.00");

        assertThrows(BusinessRuleException.class, () ->
                postService.create(PostRequest.builder()
                        .title("t").body("b").premium(false)
                        .authorId(creator.getId()).tierId(tier.getId()).build()));
    }

    @Test
    @DisplayName("Publishing on another creator's tier is rejected")
    void postOnForeignTier_throws() {
        UserResponse creatorA = newUser("creator_a");
        UserResponse creatorB = newUser("creator_b");
        SubscriptionTierResponse tierB = newTier(creatorB.getId(), "VIP", "9.99");

        assertThrows(BusinessRuleException.class, () ->
                postService.create(PostRequest.builder()
                        .title("t").body("b").premium(true)
                        .authorId(creatorA.getId()).tierId(tierB.getId()).build()));
    }

    @Test
    @DisplayName("Duplicate username is rejected")
    void duplicateUsername_throws() {
        newUser("dupe_username");
        assertThrows(DuplicateResourceException.class, () -> newUser("dupe_username"));
    }

    @Test
    @DisplayName("A tier price must be greater than zero")
    void nonPositiveTierPrice_throws() {
        UserResponse creator = newUser("creator_price");
        assertThrows(BusinessRuleException.class, () -> newTier(creator.getId(), "Free?", "0"));
    }
}
