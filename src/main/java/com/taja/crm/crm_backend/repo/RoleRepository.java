package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByCode(String code);
}
