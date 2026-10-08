package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Opportunity o where o.id = :id")
    java.util.Optional<Opportunity> findForUpdateById(String id);
}
