package com.taja.crm.crm_backend.repo;
import com.taja.crm.crm_backend.model.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SalesOrderRepository extends JpaRepository<SalesOrder, String> {}
