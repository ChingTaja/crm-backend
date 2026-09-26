package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.PasswordResetToken;
import com.taja.crm.crm_backend.repo.PasswordResetTokenRepository;
import com.taja.crm.crm_backend.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetMailService mail;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void forgotPassword(String email) {
        users.findForResetByEmail(email.strip()).ifPresent(user -> {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            tokens.invalidateForUser(user.getId());
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(15)));
            tokens.saveAndFlush(token);
            mail.send(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("密碼需至少 8 個字元，且 UTF-8 長度不可超過 72 bytes");
        }
        String hash = hash(rawToken);
        String userId = tokens.findUserIdByHash(hash).orElseThrow(this::invalidToken);
        // 所有發行及使用 token 的流程先鎖定同一個 User，避免並行重複使用。
        var user = users.findForResetById(userId).orElseThrow(this::invalidToken);
        var token = tokens.findByTokenHash(hash).orElseThrow(this::invalidToken);
        if (token.isUsed() || !token.getExpiresAt().isAfter(clock.instant())) {
            throw invalidToken();
        }
        if (encoder.matches(newPassword, user.getPasswordHash())
                || user.getPasswordHistory().stream().anyMatch(old -> encoder.matches(newPassword, old))) {
            throw new IllegalArgumentException("新密碼不能與目前密碼或前三次密碼相同");
        }
        user.getPasswordHistory().add(user.getPasswordHash());
        while (user.getPasswordHistory().size() > 3) {
            user.getPasswordHistory().removeFirst();
        }
        user.setPasswordHash(encoder.encode(newPassword));
        tokens.invalidateForUser(userId);
    }

    private IllegalArgumentException invalidToken() {
        return new IllegalArgumentException("重設連結無效、已過期或已使用");
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
