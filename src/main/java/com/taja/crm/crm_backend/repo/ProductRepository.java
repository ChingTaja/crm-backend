package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Product> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, String id);
}
