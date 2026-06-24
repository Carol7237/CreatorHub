package com.creatorhub.userservice.security;

import com.creatorhub.common.security.GatewayHeaders;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test: stateless JWT authentication + role-based authorization
 * via MockMvc, on the "test" profile (H2, no Docker, no Eureka). Not transactional:
 * registration must commit so login can authenticate against it; tests use unique
 * usernames and H2 is recreated per run.
 *
 * <p>Step 2 model: login returns a JWT (no session, NO CSRF). Protected endpoints
 * authenticate from the gateway-injected {@code X-User-*} headers (simulated here).
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
    @DisplayName("register: creates the user, stores a BCrypt hash, never returns the password (no CSRF needed)")
    void register_storesBcryptHash() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("reg_user").email("reg_user@example.com")
                .password("Secret123").displayName("Reg").build();

        mockMvc.perform(post("/api/auth/register")
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
    @DisplayName("login: correct credentials -> 200 with token + Bearer + user (no CSRF)")
    void login_correctCredentials() throws Exception {
        seedUser("login_ok", "pass12345");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("login_ok", "pass12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.username").value("login_ok"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("login: wrong credentials -> 401")
    void login_wrongCredentials() throws Exception {
        seedUser("login_bad", "rightpass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("login_bad", "WRONG"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login: returns a valid signed JWT carrying userId + username + roles")
    void login_returnsValidJwt() throws Exception {
        seedUser("jwt_user", "pass12345");
        Long id = userRepository.findByUsername("jwt_user").orElseThrow().getId();

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("jwt_user", "pass12345"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        JwtPrincipal principal = jwtService.parse(token);
        assertThat(principal.userId()).isEqualTo(id);
        assertThat(principal.username()).isEqualTo("jwt_user");
        assertThat(principal.roles()).contains("ROLE_USER");
    }

    @Test
    @DisplayName("GET /me with gateway identity headers -> 200 returns that user")
    void me_withGatewayHeaders_returnsUser() throws Exception {
        seedUser("me_user", "pass12345");
        Long id = userRepository.findByUsername("me_user").orElseThrow().getId();

        mockMvc.perform(get("/api/auth/me")
                        .header(GatewayHeaders.USER_ID, id)
                        .header(GatewayHeaders.USER_ROLES, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("me_user"));
    }

    @Test
    @DisplayName("GET /me without any identity -> 401")
    void me_withoutAuth_unauthorized() throws Exception {
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
}
