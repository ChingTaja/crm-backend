package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Contact e where e.id = :id")
    java.util.Optional<Contact> findForUpdateById(String id);
}
