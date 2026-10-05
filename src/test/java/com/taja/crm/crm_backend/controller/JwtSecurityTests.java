package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.auth.UserResponse;
import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.service.JwtService;
import com.taja.crm.crm_backend.service.PasswordResetService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.mail.from=test@example.com")
@AutoConfigureMockMvc
@Transactional
class JwtSecurityTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder passwords;
    @Autowired PasswordResetService resets;
    @MockitoBean JavaMailSender mail;
    User user;
    String token;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUsername("jwt-" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@example.com");
        user.setPasswordHash(passwords.encode("old-password"));
        user.setRole(roles.findByCode("USER").orElseThrow());
        users.saveAndFlush(user);
        token = jwtService.issue(UserResponse.fromEntity(user)).accessToken();
    }

    @Test
    void allBusinessApisRequireBearerTokenAndIgnoreOldSessions() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        for (String url : new String[]{"/api/leads", "/api/customers", "/api/contacts", "/api/users",
                "/api/entities/leads/fields", "/api/auth/me"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
            mvc.perform(get(url).session(session)).andExpect(status().isUnauthorized());
            mvc.perform(get(url).header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/leads").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    String signed(String issuer, Instant expiration) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId())
                .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(expiration)
                .claim("version", user.getTokenVersion()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void rejectsExpiredWrongIssuerAndTamperedTokens() throws Exception {
        String[] parts = token.split("\\.");
        String signature = parts[2];
        String tampered = parts[0] + "." + parts[1] + "." + (signature.charAt(0) == 'A' ? "B" : "A") + signature.substring(1);
        for (String invalid : new String[]{signed("crm-backend", Instant.now().minusSeconds(120)),
                signed("other-issuer", Instant.now().plusSeconds(1800)), tampered}) {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void resettingPasswordRevokesPreviouslyIssuedJwt() throws Exception {
        resets.forgotPassword(user.getEmail());
        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(capture.capture());
        String resetToken = capture.getValue().getText().split("token=")[1].split("\\s")[0];
        resets.resetPassword(resetToken, "new-password");
        users.flush();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingUserRevokesJwt() throws Exception {
        users.delete(user); users.flush();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
