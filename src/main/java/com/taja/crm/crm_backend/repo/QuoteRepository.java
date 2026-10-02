package com.taja.crm.crm_backend.repo;
import com.taja.crm.crm_backend.model.Quote;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface QuoteRepository extends JpaRepository<Quote, String> {
    Page<Quote> findByCreatedBy(String createdBy, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quote q where q.id = :id")
    Optional<Quote> findForUpdateById(String id);
}
