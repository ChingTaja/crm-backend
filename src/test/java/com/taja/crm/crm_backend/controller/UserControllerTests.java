package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    String adminToken;
    @Autowired com.taja.crm.crm_backend.service.JwtService jwtService;
    User admin;

    @BeforeEach
    void setup() {
        Role role = roles.findByCode("ADMIN").orElseGet(() -> {
            Role value = new Role(); value.setCode("ADMIN"); value.setName("管理員");
            return roles.saveAndFlush(value);
        });
        admin = new User();
        admin.setUsername("admin-" + UUID.randomUUID());
        admin.setEmail(admin.getUsername() + "@example.com");
        admin.setPasswordHash(encoder.encode("admin-password"));
        admin.setRole(role);
        users.saveAndFlush(admin);
        adminToken = jwtService.issue(com.taja.crm.crm_backend.dto.auth.UserResponse.fromEntity(admin)).accessToken();
    }

    @Test
    void fullCrudPreservesHashAndCleansUpRelatedData() throws Exception {
        String name = "user-" + UUID.randomUUID();
        String result = mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.com\",\"password\":\"new-password\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role.code").value("USER"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(result, "$.id");
        User user = users.findById(id).orElseThrow();
        String hash = user.getPasswordHash();
        assertTrue(encoder.matches("new-password", hash));
        user.getPasswordHistory().add(encoder.encode("previous-password"));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user); token.setTokenHash(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plusSeconds(900));
        tokens.saveAndFlush(token);
        String tokenId = token.getId();
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mvc.perform(put("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "-updated\",\"email\":\"" + name
                        + "-updated@example.com\",\"roleId\":\"" + user.getRole().getId() + "\"}"))
                .andExpect(status().isOk());
        em.flush(); em.clear();
        assertEquals(hash, users.findById(id).orElseThrow().getPasswordHash());
        assertTrue(tokens.findById(tokenId).orElseThrow().isUsed());
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(name + "-updated"));
        mvc.perform(delete("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isNoContent());
        em.clear();
        assertFalse(users.existsById(id));
        assertFalse(tokens.existsById(tokenId));
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isNotFound());
    }

    @Test
    void requiresAdminAndProtectsSelfDeletion() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/users/{id}", admin.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
        admin.setRole(roles.findByCode("USER").orElseThrow());
        users.saveAndFlush(admin);
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/users/missing").header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"new-password\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/missing").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"roleId\":\"missing\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingUserAndInvalidPaginationReturnErrors() throws Exception {
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken).param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/users/missing").header("Authorization", "Bearer " + adminToken)).andExpect(status().isNotFound());
        mvc.perform(put("/api/users/missing").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"roleId\":\"missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void disablingRevokesTokensAndReenablingRequiresNewLogin() throws Exception {
        User target = new User();
        target.setUsername("status-" + UUID.randomUUID());
        target.setEmail(target.getUsername() + "@example.com");
        target.setPasswordHash(encoder.encode("test-password"));
        target.setRole(roles.findByCode("USER").orElseThrow());
        users.saveAndFlush(target);
        assertTrue(target.isEnabled());
        String oldToken = jwtService.issue(com.taja.crm.crm_backend.dto.auth.UserResponse.fromEntity(target)).accessToken();
        for (boolean enabled : new boolean[]{false, true}) {
            mvc.perform(patch("/api/users/{id}/status", target.getId()).header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":" + enabled + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(enabled))
                    .andExpect(jsonPath("$.username").value(target.getUsername()))
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldToken))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/users/{id}", target.getId()).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(enabled));
            var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + target.getUsername() + "\",\"password\":\"test-password\"}"));
            if (!enabled) login.andExpect(status().isUnauthorized());
            else {
                String result = login.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                String newToken = JsonPath.read(result, "$.accessToken");
                mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newToken)).andExpect(status().isOk());
            }
        }
    }

    @Test
    void statusRequiresAuthenticationPermissionAndExplicitBoolean() throws Exception {
        mvc.perform(patch("/api/users/{id}/status", admin.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/users/{id}/status", admin.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/users/{id}/status", admin.getId()).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_DISABLE_FORBIDDEN"));
        mvc.perform(patch("/api/users/missing/status").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isNotFound());
        admin.setRole(roles.findByCode("USER").orElseThrow());
        users.saveAndFlush(admin);
        mvc.perform(patch("/api/users/missing/status").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
    }
}
