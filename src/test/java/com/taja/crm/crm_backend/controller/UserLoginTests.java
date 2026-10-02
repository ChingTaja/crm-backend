package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.repo.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
class UserLoginTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired com.taja.crm.crm_backend.service.JwtService jwtService;
    @Autowired com.taja.crm.crm_backend.repo.RoleRepository roles;

    String register(String password) throws Exception {
        String name = "account-" + UUID.randomUUID();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"email\":\"" + name
                        + "@example.com\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.role.code").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        return name;
    }

    @Test
    void bcryptUsesDifferentSaltForIdenticalPasswords() throws Exception {
        String first = register("same-password");
        String second = register("same-password");
        String firstHash = users.findByUsername(first).orElseThrow().getPasswordHash();
        String secondHash = users.findByUsername(second).orElseThrow().getPasswordHash();
        assertNotEquals(firstHash, secondHash);
        assertTrue(encoder.matches("same-password", firstHash));
        assertTrue(encoder.matches("same-password", secondHash));
    }

    @Test
    void loginWithNonEmailUsernameIssuesJwtAndLogoutRevokesIt() throws Exception {
        String name = register("good-password");
        var result = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"password\":\"good-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        assertNull(result.getRequest().getSession(false));
        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(name));
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownUserReturnUnauthorized() throws Exception {
        String name = register("good-password");
        for (String username : new String[]{name, "missing-" + UUID.randomUUID()}) {
            var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized()).andReturn();
            assertNull(result.getRequest().getSession(false));
        }
    }

    @Test
    void duplicateAccountReturnsConflict() throws Exception {
        String name = register("good-password");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"email\":\"other@example.com\",\"password\":\"good-password\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationPersistsRoleForeignKey() throws Exception {
        String name = register("good-password");
        var user = users.findByUsername(name).orElseThrow();
        assertEquals(roles.findByCode("USER").orElseThrow().getId(), user.getRole().getId());
    }

    @Test
    void onlyAdminCanAssignPrivilegedRole() throws Exception {
        var adminRole = roles.findByCode("ADMIN").orElseGet(() -> {
            var role = new com.taja.crm.crm_backend.model.Role();
            role.setCode("ADMIN");
            role.setName("管理員");
            return roles.saveAndFlush(role);
        });
        String name = "assigned-" + UUID.randomUUID();
        String payload = "{\"username\":\"" + name + "\",\"email\":\"" + name
                + "@example.com\",\"password\":\"good-password\",\"roleId\":\"" + adminRole.getId() + "\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        String actorName = register("good-password");
        var actor = users.findByUsername(actorName).orElseThrow();
        String token = jwtService.issue(com.taja.crm.crm_backend.dto.auth.UserResponse.fromEntity(actor)).accessToken();
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        actor.setRole(adminRole);
        users.saveAndFlush(actor);
        mvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role.id").value(adminRole.getId()))
                .andExpect(jsonPath("$.role.code").value("ADMIN"));
    }

    @Test
    void unknownRoleIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"good-password\",\"roleId\":\"missing\"}"))
                .andExpect(status().isBadRequest());
    }
}
