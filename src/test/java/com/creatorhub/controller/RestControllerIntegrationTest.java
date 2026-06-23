package com.creatorhub.controller;

import com.creatorhub.common.Viewer;
import com.creatorhub.dto.PostRequest;
import com.creatorhub.dto.SubscriptionTierRequest;
import com.creatorhub.dto.UserRequest;
import com.creatorhub.dto.UserResponse;
import com.creatorhub.model.enums.Role;
import com.creatorhub.service.PostService;
import com.creatorhub.service.SubscriptionTierService;
import com.creatorhub.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level integration tests (MockMvc, test profile / H2): validation,
 * error handling, authorization, CRUD verbs, and premium gating over HTTP.
 * Not transactional — seeded users must be visible to the request thread.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserService userService;
    @Autowired private SubscriptionTierService tierService;
    @Autowired private PostService postService;

    private UserResponse seedUser(String username) {
        return userService.create(UserRequest.builder()
                .username(username).email(username + "@example.com")
                .password("pass12345").role(Role.USER).build());
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Test
    @WithMockUser(username = "rc_create", roles = {"USER"})
    void createPost_valid_returns201() throws Exception {
        seedUser("rc_create");
        String body = json(PostRequest.builder().title("Hello").body("world").premium(false).build());
        mockMvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.creatorId").exists());
    }

    @Test
    @WithMockUser(username = "rc_val", roles = {"USER"})
    void createTier_invalid_returns400WithFieldErrors() throws Exception {
        // blank name + price below the minimum -> two field errors
        String body = "{\"name\":\"\",\"priceMonthly\":0}";
        mockMvc.perform(post("/api/tiers").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.priceMonthly").exists());
    }

    @Test
    @WithMockUser(username = "rc_malformed", roles = {"USER"})
    void createPost_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest()); // not 500 (Phase 5 limitation fixed)
    }

    @Test
    void createPost_withoutAuth_returns401() throws Exception {
        String body = json(PostRequest.builder().title("x").body("y").premium(false).build());
        mockMvc.perform(post("/api/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "rc_user", roles = {"USER"})
    void adminEndpoint_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    void getPost_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/999999")).andExpect(status().isNotFound());
    }

    @Test
    void getPremiumPost_anonymous_isLocked() throws Exception {
        Long creatorId = seedUser("rc_gate").getId();
        Viewer creator = new Viewer(creatorId, false);
        Long tierId = tierService.create(SubscriptionTierRequest.builder()
                .name("VIP").priceMonthly(new BigDecimal("9.99")).build(), creator).getId();
        Long postId = postService.create(PostRequest.builder()
                .title("Secret").body("hidden").premium(true).tierId(tierId).build(), creator).getId();

        mockMvc.perform(get("/api/posts/" + postId)) // anonymous
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.title").value("Secret"))
                .andExpect(jsonPath("$.body").doesNotExist());
    }

    @Test
    void getPosts_publicList_returns200() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }
}
