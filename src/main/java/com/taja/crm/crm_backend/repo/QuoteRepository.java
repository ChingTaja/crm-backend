package com.taja.crm.crm_backend.repo;
import com.taja.crm.crm_backend.model.Quote;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface QuoteRepository extends JpaRepository<Quote, String> {
    Page<Quote> findByCreatedBy(String createdBy, Pageable pageable);
    @Query("select (count(v) > 0) from QuoteVersion v where v.terms.opportunity.id = :opportunityId")
    boolean existsForOpportunity(String opportunityId);
    @Query("""
        select q from Quote q join q.versions v
        where (:allVisible = true or q.createdBy = :actorId)
          and (:opportunityId is null or v.terms.opportunity.id = :opportunityId)
          and v.version = (select max(v2.version) from QuoteVersion v2 where v2.quote = q)
        """)
    Page<Quote> findVisibleQuotes(String actorId, boolean allVisible, String opportunityId, Pageable pageable);
    @Query(value = """
        select q from Quote q join q.versions v
        where v.reviewerId = :actorId and v.approval = com.taja.crm.crm_backend.model.ApprovalStatus.Pending
          and v.version = (select max(v2.version) from QuoteVersion v2 where v2.quote = q)
        order by v.approvalRequestedAt asc, q.id asc
        """, countQuery = """
        select count(q) from Quote q join q.versions v
        where v.reviewerId = :actorId and v.approval = com.taja.crm.crm_backend.model.ApprovalStatus.Pending
          and v.version = (select max(v2.version) from QuoteVersion v2 where v2.quote = q)
        """)
    Page<Quote> findPendingReviews(String actorId, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quote q where q.id = :id")
    Optional<Quote> findForUpdateById(String id);
}
