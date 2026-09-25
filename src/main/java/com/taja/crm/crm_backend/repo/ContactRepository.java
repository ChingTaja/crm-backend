package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, String> {
}
