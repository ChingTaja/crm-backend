package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.dto.user.UpdateUserRequest;
import com.taja.crm.crm_backend.model.User;
import com.taja.crm.crm_backend.repo.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordResetTokenRepository tokens;
    private final UserAuthService auth;

    private void requireAdmin(String actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
        }
        User actor = users.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請重新登入"));
        if (actor.getRole() == null || !"ADMIN".equals(actor.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有管理員可以管理使用者");
        }
    }

    public Page<UserResponse> findAllUsers(String actorId, Pageable pageable) {
        requireAdmin(actorId);
        return users.findAll(pageable).map(UserResponse::fromEntity);
    }

    public UserResponse findByIdUser(String actorId, String id) {
        requireAdmin(actorId);
        return UserResponse.fromEntity(findUser(id));
    }

    @Transactional
    public UserResponse createUsers(String actorId, RegisterRequest request) {
        requireAdmin(actorId);
        return auth.register(request, actorId);
    }

    @Transactional
    public UserResponse updateUsers(String actorId, String id, UpdateUserRequest request) {
        requireAdmin(actorId);
        User user = users.findForResetById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
        String username = request.username().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameAndIdNot(username, id) || users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號或 email 已被使用");
        }
        var role = roles.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定角色"));
        if (id.equals(actorId) && !"ADMIN".equals(role.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能移除自己的管理員角色");
        }
        if (!user.getEmail().equalsIgnoreCase(email)) {
            tokens.invalidateForUser(id);
        }
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        return UserResponse.fromEntity(users.saveAndFlush(user));
    }

    @Transactional
    public void deleteUsers(String actorId, String id) {
        requireAdmin(actorId);
        if (id.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能刪除目前登入的管理員");
        }
        User user = users.findForResetById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
        tokens.deleteByUserId(id);
        users.delete(user);
        users.flush();
    }

    private User findUser(String id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
    }
}
