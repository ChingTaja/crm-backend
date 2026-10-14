package com.taja.crm.crm_backend.repo;

import com.taja.crm.crm_backend.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {}
