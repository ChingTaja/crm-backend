package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from Lead l where l.id = :id")
    java.util.Optional<Lead> findForUpdateById(String id);
}
