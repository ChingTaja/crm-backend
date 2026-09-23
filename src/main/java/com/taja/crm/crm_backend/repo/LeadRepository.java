package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, String> {
}
