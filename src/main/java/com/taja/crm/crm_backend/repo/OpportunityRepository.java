package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, String> {}
