package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.UserRepository;
import com.taja.crm.crm_backend.repo.RoleRepository;
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
    private final RoleRepository roles;
    private final String dummyHash;

    public UserAuthService(UserRepository users, PasswordEncoder encoder, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional
    public UserResponse register(RegisterRequest request, String actorId) {
        String username = request.username().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密碼 UTF-8 長度不可超過 72 bytes");
        }
        if (users.existsByUsername(username) || users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號或 email 已被使用");
        }
        var role = request.roleId() == null
                ? roles.findByCode("USER").orElseThrow(() -> new IllegalStateException("缺少預設 USER 角色"))
                : roles.findById(request.roleId()).orElseThrow(() -> new IllegalArgumentException("找不到指定角色"));
        if (!"USER".equals(role.getCode())) {
            boolean admin = actorId != null && users.findById(actorId)
                    .map(User::getRole).map(r -> "ADMIN".equals(r.getCode())).orElse(false);
            if (!admin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以指定其他角色");
            }
        }
        User user = new User();
        user.setRole(role);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        return UserResponse.fromEntity(users.saveAndFlush(user));
    }

    @Transactional
    public UserResponse login(LoginRequest request) {
        var user = users.findForLoginByUsername(request.username().strip());
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
