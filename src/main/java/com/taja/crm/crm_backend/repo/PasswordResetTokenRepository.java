package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    @Query("select t.user.id from PasswordResetToken t where t.tokenHash = :hash")
    Optional<String> findUserIdByHash(String hash);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
    void invalidateForUser(String userId);
}
