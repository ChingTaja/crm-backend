package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
