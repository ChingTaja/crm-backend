package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserAuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public UserAuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密碼 UTF-8 長度不可超過 72 bytes");
        }
        if (users.existsByUsername(username) || users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號或 email 已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        return UserResponse.fromEntity(users.saveAndFlush(user));
    }

    public UserResponse login(LoginRequest request) {
        var user = users.findByUsername(request.username().strip());
        boolean matches = request.password().getBytes(StandardCharsets.UTF_8).length <= 72
                && encoder.matches(request.password(), user.map(User::getPasswordHash).orElse(dummyHash));
        if (user.isEmpty() || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
        }
        return UserResponse.fromEntity(user.orElseThrow());
    }

    public UserResponse findCurrentUser(String id) {
        return users.findById(id).map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請重新登入"));
    }
}
