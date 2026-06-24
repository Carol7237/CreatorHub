package com.creatorhub.userservice.security;

import com.creatorhub.security.jwt.JwtPrincipal;
import com.creatorhub.security.jwt.JwtService;
import com.creatorhub.userservice.dto.LoginRequest;
import com.creatorhub.userservice.dto.RegisterRequest;
import com.creatorhub.userservice.dto.UserRequest;
import com.creatorhub.userservice.model.User;
import com.creatorhub.userservice.model.enums.Role;
import com.creatorhub.userservice.repository.UserRepository;
import com.creatorhub.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test: authentication + role-based authorization via
 * MockMvc, on the "test" profile (H2, no Docker, no Eureka). Not transactional:
 * registration must commit so the login flow can authenticate against it; tests
 * use unique usernames and H2 is recreated per run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    private void seedUser(String username, String password) {
        userService.create(UserRequest.builder()
                .username(username).email(username + "@example.com")
                .password(password).role(Role.USER).build());
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Test
    @DisplayName("register: creates the user, stores a BCrypt hash, never returns the password")
    void register_storesBcryptHash() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("reg_user").email("reg_user@example.com")
                .password("Secret123").displayName("Reg").build();

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("reg_user"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findByUsername("reg_user").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("Secret123");
        assertThat(stored.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("Secret123", stored.getPassword())).isTrue();
    }

    @Test
    @DisplayName("login: correct credentials -> 200 with token + Bearer + user")
    void login_correctCredentials() throws Exception {
        seedUser("login_ok", "pass12345");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("login_ok", "pass12345", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.username").value("login_ok"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("login: returns a valid signed JWT carrying userId + username + roles")
    void login_returnsValidJwt() throws Exception {
        seedUser("jwt_user", "pass12345");
        Long id = userRepository.findByUsername("jwt_user").orElseThrow().getId();

        String body = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("jwt_user", "pass12345", false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        JwtPrincipal principal = jwtService.parse(token);
        assertThat(principal.userId()).isEqualTo(id);
        assertThat(principal.username()).isEqualTo("jwt_user");
        assertThat(principal.roles()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("login: wrong credentials -> 401")
    void login_wrongCredentials() throws Exception {
        seedUser("login_bad", "rightpass");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("login_bad", "WRONG", false))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login with remember-me -> issues the remember-me cookie")
    void login_rememberMe_setsCookie() throws Exception {
        seedUser("rm_user", "pass12345");
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("rm_user", "pass12345", true))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    @DisplayName("protected endpoint without authentication -> 401")
    void protectedEndpoint_noAuth() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("admin endpoint as USER -> 403")
    @WithMockUser(username = "regular", roles = {"USER"})
    void adminEndpoint_asUser_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin endpoint as ADMIN -> 200")
    @WithMockUser(username = "boss", roles = {"ADMIN"})
    void adminEndpoint_asAdmin_ok() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout with CSRF -> 200")
    @WithMockUser
    void logout_ok() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("logout WITHOUT CSRF -> rejected (CSRF enforced)")
    @WithMockUser
    void logout_withoutCsrf_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("register WITHOUT CSRF -> rejected (CSRF enforced)")
    void register_withoutCsrf_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(RegisterRequest.builder()
                                .username("nocsrf").email("nocsrf@example.com").password("pass12345").build())))
                .andExpect(status().isForbidden());
    }
}
