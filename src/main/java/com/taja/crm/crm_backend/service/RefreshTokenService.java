package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final UserAuthService auth;
    private final JwtService jwt;
    private final Clock clock;
    private final Duration ttl;
    private final EntityManager em;
    private final SecureRandom random = new SecureRandom();
    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, UserAuthService auth,
            JwtService jwt, Clock clock, EntityManager em, @Value("${app.refresh.ttl:P7D}") Duration ttl) {
        if(ttl.isZero() || ttl.isNegative()) throw new IllegalArgumentException("Refresh TTL must be positive");
        this.tokens=tokens;this.users=users;this.auth=auth;this.jwt=jwt;this.clock=clock;this.em=em;this.ttl=ttl;
    }
    public record Grant(LoginResponse login, String refreshToken, Instant expiresAt) {}
    public static String hash(String raw) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private Grant grant(User user, String family, Instant expires) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token=new RefreshToken();token.setTokenHash(hash(raw));token.setUserId(user.getId());
        token.setFamilyId(family);token.setExpiresAt(expires);token.setTokenVersion(user.getTokenVersion());
        tokens.saveAndFlush(token);
        return new Grant(jwt.issue(UserResponse.fromEntity(user)),raw,expires);
    }
    @Transactional
    public Grant login(LoginRequest request) {
        UserResponse response=auth.login(request); // Holds the user write lock through token issuance.
        User user=users.findById(response.id()).orElseThrow();
        return grant(user,UUID.randomUUID().toString(),clock.instant().plus(ttl));
    }
    @Transactional(noRollbackFor=RefreshRejected.class)
    public Grant refresh(String raw) {
        RefreshToken token=lookup(raw).orElseThrow(()->rejected("REFRESH_TOKEN_INVALID"));
        User user=users.findForResetById(token.getUserId()).orElseThrow(()->rejected("REFRESH_TOKEN_INVALID"));
        em.refresh(token,LockModeType.PESSIMISTIC_WRITE);
        if(!user.isEnabled()) throw new RefreshRejected("ACCOUNT_DISABLED","帳號已停用，請聯絡管理員。");
        if(token.getTokenVersion()!=user.getTokenVersion() || token.isRevoked()) throw rejected("REFRESH_TOKEN_REVOKED");
        if(!token.getExpiresAt().isAfter(clock.instant())) throw rejected("REFRESH_TOKEN_EXPIRED");
        if(token.getUsedAt()!=null) {
            tokens.revokeFamily(token.getFamilyId());
            // Also invalidate already issued access tokens, preserving existing account-wide logout semantics.
            user.setTokenVersion(user.getTokenVersion()+1);
            throw rejected("REFRESH_TOKEN_REUSED");
        }
        token.setUsedAt(clock.instant());
        return grant(user,token.getFamilyId(),token.getExpiresAt());
    }
    @Transactional
    public void logout(String raw) {
        lookup(raw).ifPresent(token -> users.findForResetById(token.getUserId()).ifPresent(user -> {
            if(token.getTokenVersion()==user.getTokenVersion()) user.setTokenVersion(user.getTokenVersion()+1);
            tokens.revokeFamily(token.getFamilyId());
        }));
    }
    private Optional<RefreshToken> lookup(String raw) {
        if(raw==null || !raw.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return tokens.findByTokenHash(hash(raw));
    }
    private RefreshRejected rejected(String code) {return new RefreshRejected(code,"登入已失效，請重新登入。");}
}
