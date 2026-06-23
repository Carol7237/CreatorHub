package com.creatorhub.service;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PagedResponse;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.PostResponse;
import com.creatorhub.dto.SubscriptionRequest;
import com.creatorhub.dto.SubscriptionResponse;
import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.exception.BusinessRuleException;
import com.creatorhub.model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end integration test (Scenario 3): pagination & sorting through the real
 * service layer + H2 (test profile, rollback per test).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaginationSortingIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private PostService postService;
    @Autowired
    private SubscriptionTierService tierService;
    @Autowired
    private SubscriptionService subscriptionService;

    private static Viewer viewer(Long id) {
        return new Viewer(id, false);
    }

    private Long createUser(String username) {
        return userService.create(UserRequest.builder()
                .username(username).email(username + "@example.com")
                .password("pass12345").role(Role.USER).build()).getId();
    }

    private void createPosts(Long creatorId, int count) {
        for (int i = 1; i <= count; i++) {
            postService.create(PostRequest.builder()
                    .title(String.format("Post %02d", i))
                    .body("body " + i)
                    .premium(false)
                    .build(), viewer(creatorId));
        }
    }

    @Test
    @DisplayName("25 posts paginate as 20 + 5 across 2 pages")
    void postPagination_returnsCorrectPages() {
        createPosts(createUser("pager"), 25);

        PagedResponse<PostResponse> page0 = postService.findAll(PageRequest.of(0, 20), Viewer.anonymous());
        assertThat(page0.getContent()).hasSize(20);
        assertThat(page0.getTotalElements()).isEqualTo(25);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.isFirst()).isTrue();
        assertThat(page0.isLast()).isFalse();

        PagedResponse<PostResponse> page1 = postService.findAll(PageRequest.of(1, 20), Viewer.anonymous());
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.isFirst()).isFalse();
        assertThat(page1.isLast()).isTrue();
    }

    @Test
    @DisplayName("Sorting posts by title asc vs desc yields opposite first elements")
    void postSorting_byTitle() {
        createPosts(createUser("sorter"), 25);

        PagedResponse<PostResponse> asc =
                postService.findAll(PageRequest.of(0, 25, Sort.by("title").ascending()), Viewer.anonymous());
        PagedResponse<PostResponse> desc =
                postService.findAll(PageRequest.of(0, 25, Sort.by("title").descending()), Viewer.anonymous());

        assertThat(asc.getContent().get(0).getTitle()).isEqualTo("Post 01");
        assertThat(desc.getContent().get(0).getTitle()).isEqualTo("Post 25");
    }

    @Test
    @DisplayName("Page size is capped at the maximum (100)")
    void maxPageSize_isCapped() {
        createPosts(createUser("capper"), 25);

        PagedResponse<PostResponse> capped = postService.findAll(PageRequest.of(0, 500), Viewer.anonymous());
        assertThat(capped.getSize()).isEqualTo(100); // requested 500, capped to MAX_PAGE_SIZE
        assertThat(capped.getContent()).hasSize(25);
    }

    @Test
    @DisplayName("Sorting by a non-whitelisted property is rejected")
    void invalidSortProperty_throws() {
        createUser("badsort");
        assertThrows(BusinessRuleException.class, () ->
                postService.findAll(PageRequest.of(0, 10, Sort.by("nonexistent")), Viewer.anonymous()));
    }

    @Test
    @DisplayName("SECURITY: users cannot be sorted by password")
    void sortUsersByPassword_blocked() {
        createUser("seccheck");
        assertThrows(BusinessRuleException.class, () ->
                userService.findAll(PageRequest.of(0, 10, Sort.by("password"))));
    }

    @Test
    @DisplayName("Subscriptions paginate correctly")
    void subscriptionPagination() {
        Long creatorId = createUser("sub_creator");
        Long tierId = tierService.create(SubscriptionTierRequest.builder()
                .name("VIP").priceMonthly(new BigDecimal("9.99")).build(), viewer(creatorId)).getId();

        for (int i = 0; i < 3; i++) {
            Long fanId = createUser("fan_" + i);
            subscriptionService.create(SubscriptionRequest.builder().tierId(tierId).build(), viewer(fanId));
        }

        PagedResponse<SubscriptionResponse> page0 =
                subscriptionService.findAll(PageRequest.of(0, 2, Sort.by("startDate").descending()));
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page0.getTotalPages()).isEqualTo(2);
    }
}
