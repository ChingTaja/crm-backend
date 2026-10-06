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

    private final PermissionService access;

    public Page<UserResponse> findAllUsers(String actorId, Pageable pageable) {
        access.require(actorId, "users.read");
        return users.findAll(pageable).map(UserResponse::fromEntity);
    }

    public UserResponse findByIdUser(String actorId, String id) {
        access.require(actorId, "users.read");
        return UserResponse.fromEntity(findUser(id));
    }

    @Transactional
    public UserResponse createUsers(String actorId, RegisterRequest request) {
        access.require(actorId, "users.create");
        return auth.register(request, actorId);
    }

    @Transactional
    public UserResponse updateUsers(String actorId, String id, UpdateUserRequest request) {
        access.lockAdministration();
        access.require(actorId, "users.update");
        User user = users.findForResetById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
        access.manageTarget(actorId, user);
        String username = request.username().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByUsernameAndIdNot(username, id) || users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號或 email 已被使用");
        }
        var role = request.roleId() == null ? user.getRole() : roles.findById(request.roleId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定角色"));
        String beforeRole = user.getRole() == null ? null : user.getRole().getId();
        boolean changed = !java.util.Objects.equals(beforeRole, role == null ? null : role.getId());
        if (changed) {
            access.require(actorId, "users.assign-role");
            if (id.equals(actorId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "不能改派自己的角色");
            access.requireAssignable(actorId, role);
            if (user.isEnabled() && user.getRole() != null && "ADMIN".equals(user.getRole().getCode()) && users.countByRoleCodeAndEnabledTrue("ADMIN") <= 1)
                throw new QuoteException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTED", "不能移除最後一位管理員。");
            access.audit(actorId, "UserRoleAssigned", id, beforeRole, role.getId());
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
        access.lockAdministration();
        access.require(actorId, "users.delete");
        if (id.equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能刪除目前登入的管理員");
        }
        User user = users.findForResetById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
        access.manageTarget(actorId, user);
        if (user.isEnabled() && user.getRole() != null && "ADMIN".equals(user.getRole().getCode()) && users.countByRoleCodeAndEnabledTrue("ADMIN") <= 1)
            throw new QuoteException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTED", "不能刪除最後一位管理員。");
        access.audit(actorId, "UserDeleted", id, user.getRole() == null ? null : user.getRole().getId(), null);
        tokens.deleteByUserId(id);
        users.delete(user);
        users.flush();
    }

    @Transactional
    public UserResponse updateUserStatus(String actorId, String id, boolean enabled) {
        access.lockAdministration();
        access.require(actorId, "users.update");
        User user = users.findForResetById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
        if (!enabled && id.equals(actorId))
            throw new QuoteException(HttpStatus.CONFLICT, "SELF_DISABLE_FORBIDDEN", "不能停用自己的帳號。");
        if (!enabled && user.isEnabled() && user.getRole() != null
                && "ADMIN".equals(user.getRole().getCode()) && users.countByRoleCodeAndEnabledTrue("ADMIN") <= 1)
            throw new QuoteException(HttpStatus.CONFLICT, "LAST_ADMIN_PROTECTED", "不能停用最後一位有效管理員。");
        access.manageTarget(actorId, user);
        if (user.isEnabled() != enabled) {
            access.audit(actorId, "UserStatusChanged", id, Boolean.toString(user.isEnabled()), Boolean.toString(enabled));
            user.setEnabled(enabled);
            user.setTokenVersion(user.getTokenVersion() + 1);
            if (!enabled) tokens.invalidateForUser(id);
        }
        return UserResponse.fromEntity(users.saveAndFlush(user));
    }

    private User findUser(String id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 User：" + id));
    }
}
