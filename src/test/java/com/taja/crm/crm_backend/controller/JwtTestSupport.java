package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.dto.auth.UserResponse;
import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.UserRepository;
import com.taja.crm.crm_backend.service.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
abstract class JwtTestSupport {
    @Autowired private UserRepository authUsers;
    @Autowired private PasswordEncoder authPasswords;
    @Autowired private JwtService authJwt;
    protected String bearerToken;

    @BeforeEach
    void authenticateRequests() {
        User user = new User();
        user.setUsername("api-test-" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@example.com");
        user.setPasswordHash(authPasswords.encode("test-password"));
        authUsers.saveAndFlush(user);
        bearerToken = "Bearer " + authJwt.issue(UserResponse.fromEntity(user)).accessToken();
    }
}
