package com.taja.crm.crm_backend.repo;
import com.taja.crm.crm_backend.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @Modifying @Query("delete from RefreshToken t where t.userId=:userId")
    void deleteByUserId(String userId);
    @Modifying @Query("update RefreshToken t set t.revoked=true where t.familyId=:familyId")
    void revokeFamily(String familyId);
}
