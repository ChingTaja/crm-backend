package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Customer e where e.id = :id")
    java.util.Optional<Customer> findForUpdateById(String id);
}
