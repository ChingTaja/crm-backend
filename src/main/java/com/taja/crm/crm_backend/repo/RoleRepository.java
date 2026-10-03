package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Role r where r.code = 'ADMIN'")
    Optional<Role> lockAdministration();
    @org.springframework.data.jpa.repository.Query("select r from Role r where :keyword = '' or lower(r.code) like :keyword escape '!' or lower(r.name) like :keyword escape '!'")
    org.springframework.data.domain.Page<Role> search(String keyword, org.springframework.data.domain.Pageable pageable);
    Optional<Role> findByCode(String code);
}
