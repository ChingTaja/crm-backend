package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.auth.*;
import com.taja.crm.crm_backend.repo.UserRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final UserAuthService auth;
    private final UserRepository users;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public JwtService(JwtEncoder encoder, UserRepository users, Clock clock, UserAuthService auth,
            @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.ttl}") Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("JWT TTL 必須大於 0");
        this.auth = auth;
        this.encoder = encoder; this.users = users; this.clock = clock; this.issuer = issuer; this.ttl = ttl;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 密碼驗證與 JWT 簽發共用鎖及交易，避免密碼重設並行時簽發新的有效 token。
        return issue(auth.login(request));
    }

    @Transactional(readOnly = true)
    public LoginResponse issue(UserResponse response) {
        var user = users.findById(response.id()).orElseThrow();
        if (!user.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號已停用，請聯絡管理員。");
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(user.getId())
                .issuedAt(now).expiresAt(now.plus(ttl)).claim("version", user.getTokenVersion()).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", ttl.toSeconds(), response);
    }

    /** 登出使該帳號所有已簽發的 JWT 失效。 */
    @Transactional
    public void logout(String userId) {
        var user = users.findForResetById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }
}
