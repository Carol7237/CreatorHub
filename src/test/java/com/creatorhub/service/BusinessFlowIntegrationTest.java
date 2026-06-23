package com.creatorhub.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.CommentRequest;
import com.creatorhub.dto.CommentResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end integration test (Scenario 1): the full business flow through the
 * real service layer, wired by Spring, against H2 (no Docker). Rolls back after
 * each test (@Transactional). Owners are passed explicitly via {@link Viewer}
 * (the controllers resolve this from the security context).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusinessFlowIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private com.creatorhub.service.SubscriptionTierService tierService;
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
                .password("secret12")
                .build());
    }

    private static Viewer viewer(Long userId) {
        return new Viewer(userId, false);
    }

    private SubscriptionTierResponse newTier(Long creatorId, String name, String price) {
        return tierService.create(com.creatorhub.dto.SubscriptionTierRequest.builder()
                .name(name)
                .priceMonthly(new BigDecimal(price))
                .build(), viewer(creatorId));
    }

    // --- tests ---

    @Test
    @DisplayName("Creating a user auto-creates its profile (displayName defaults to username)")
    void createUser_autoCreatesProfile() {
        UserResponse user = newUser("creator_profile");
        assertThat(user.getProfileId()).isNotNull();
        assertThat(user.getDisplayName()).isEqualTo("creator_profile");
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
                .tierId(tier.getId())
                .tags(Set.of("news", "update"))
                .build(), viewer(creator.getId()));

        assertThat(post.isPremium()).isTrue();
        assertThat(post.getTierId()).isEqualTo(tier.getId());
        assertThat(post.getCreatorId()).isEqualTo(creator.getId());
        assertThat(post.getTags()).containsExactlyInAnyOrder("news", "update");

        UserResponse fan = newUser("fan_flow");
        SubscriptionResponse sub = subscriptionService.create(
                SubscriptionRequest.builder().tierId(tier.getId()).build(), viewer(fan.getId()));

        assertThat(sub.getStatus()).isEqualTo(SubStatus.ACTIVE);
        assertThat(sub.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(sub.getCreatorId()).isEqualTo(creator.getId());

        // Subscriber can comment on the premium post and can see its body.
        CommentResponse comment = commentService.create(
                CommentRequest.builder().text("Loved this!").postId(post.getId()).build(), viewer(fan.getId()));
        assertThat(comment.getId()).isNotNull();

        PostResponse asSubscriber = postService.findById(post.getId(), viewer(fan.getId()));
        assertThat(asSubscriber.isLocked()).isFalse();
        assertThat(asSubscriber.getBody()).isEqualTo("Premium content body");

        // Anonymous viewer sees the premium post locked (no body).
        PostResponse asAnon = postService.findById(post.getId(), Viewer.anonymous());
        assertThat(asAnon.isLocked()).isTrue();
        assertThat(asAnon.getBody()).isNull();

        assertThat(commentService.findByPost(post.getId())).hasSize(1);
        assertThat(subscriptionService.findByFan(fan.getId())).hasSize(1);
    }

    @Test
    @DisplayName("A non-subscriber cannot comment on a premium post")
    void nonSubscriberCannotCommentOnPremium() {
        UserResponse creator = newUser("creator_pc");
        SubscriptionTierResponse tier = newTier(creator.getId(), "VIP", "9.99");
        PostResponse post = postService.create(PostRequest.builder()
                .title("p").body("b").premium(true).tierId(tier.getId()).build(), viewer(creator.getId()));
        UserResponse outsider = newUser("outsider_pc");

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                commentService.create(CommentRequest.builder().text("hi").postId(post.getId()).build(),
                        viewer(outsider.getId())));
    }

    @Test
    @DisplayName("A second active subscription to the same tier is rejected")
    void duplicateActiveSubscription_throws() {
        UserResponse creator = newUser("creator_dupsub");
        SubscriptionTierResponse tier = newTier(creator.getId(), "Basic", "5.00");
        UserResponse fan = newUser("fan_dupsub");

        subscriptionService.create(SubscriptionRequest.builder().tierId(tier.getId()).build(), viewer(fan.getId()));

        assertThrows(DuplicateResourceException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(tier.getId()).build(), viewer(fan.getId())));
    }

    @Test
    @DisplayName("A creator cannot subscribe to their own tier")
    void selfSubscription_throws() {
        UserResponse creator = newUser("creator_self");
        SubscriptionTierResponse tier = newTier(creator.getId(), "Basic", "5.00");

        assertThrows(BusinessRuleException.class, () -> subscriptionService.create(
                SubscriptionRequest.builder().tierId(tier.getId()).build(), viewer(creator.getId())));
    }

    @Test
    @DisplayName("A premium post without a tier is rejected")
    void premiumPostWithoutTier_throws() {
        UserResponse creator = newUser("creator_premnotier");
        assertThrows(BusinessRuleException.class, () -> postService.create(
                PostRequest.builder().title("t").body("b").premium(true).build(), viewer(creator.getId())));
    }

    @Test
    @DisplayName("Publishing on another creator's tier is rejected")
    void postOnForeignTier_throws() {
        UserResponse creatorA = newUser("creator_a");
        UserResponse creatorB = newUser("creator_b");
        SubscriptionTierResponse tierB = newTier(creatorB.getId(), "VIP", "9.99");

        assertThrows(BusinessRuleException.class, () -> postService.create(
                PostRequest.builder().title("t").body("b").premium(true).tierId(tierB.getId()).build(),
                viewer(creatorA.getId())));
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
