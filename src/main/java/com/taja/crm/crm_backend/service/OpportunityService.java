package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Opportunity;
import com.taja.crm.crm_backend.repo.OpportunityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final com.taja.crm.crm_backend.repo.CustomerRepository customers;
    private final com.taja.crm.crm_backend.repo.LeadRepository leads;
    private final com.taja.crm.crm_backend.repo.QuoteRepository quotes;
    private final PermissionService access;
    private final java.time.Clock clock;

    public Page<Opportunity> findAllOpportunities(Pageable pageable) {
        return opportunityRepository.findAll(pageable);
    }

    public Opportunity findByIdOpportunity(String id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Opportunity：" + id));
    }

    @Transactional
    public Opportunity createOpportunities(@NotNull @Valid Opportunity opportunity) {
        if (opportunity.getId() != null) {
            throw new IllegalArgumentException("新增 Opportunity 時不可指定 id");
        }
        opportunity.setStage("需求討論中");
        validateReferences(opportunity);
        return opportunityRepository.save(opportunity);
    }

    /** 完整更新指定 Opportunity，未提供的欄位會清空。 */
    @Transactional
    public Opportunity updateOpportunities(String id, @NotNull @Valid Opportunity opportunity) {
        Opportunity existing = locked(id);
        requireOpen(existing);
        if (!existing.getCustomerId().equals(opportunity.getCustomerId()) && quotes.existsForOpportunity(id))
            throw new QuoteException(org.springframework.http.HttpStatus.CONFLICT, "OPPORTUNITY_CUSTOMER_CONFLICT", "商機已有關聯報價，不可更換客戶。");
        validateReferences(opportunity);
        existing.setName(opportunity.getName());
        existing.setCustomerId(opportunity.getCustomerId());
        existing.setLeadId(opportunity.getLeadId());
        existing.setAmount(opportunity.getAmount());
        existing.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        existing.setOwner(opportunity.getOwner());
        return opportunityRepository.save(existing);
    }

    @Transactional
    public void deleteOpportunities(String id) {
        Opportunity opportunity = locked(id);
        requireOpen(opportunity);
        if (quotes.existsForOpportunity(id))
            throw new QuoteException(org.springframework.http.HttpStatus.CONFLICT, "OPPORTUNITY_HAS_QUOTES", "商機已有報價，不可直接刪除。");
        opportunityRepository.delete(opportunity);
    }
    @Transactional
    public Opportunity closeOpportunity(String actorId, String id, @Valid com.taja.crm.crm_backend.dto.opportunity.CloseOpportunityRequest request) {
        access.require(actorId, "opportunities.update");
        Opportunity value = locked(id);
        requireOpen(value);
        var actor = access.actor(actorId);
        value.setStage("won".equals(request.outcome()) ? "需求成交" : "失單");
        value.setCloseDescription(request.description() == null ? null : request.description().strip());
        value.setClosedAt(clock.instant());
        value.setClosedById(actor.getId());
        value.setClosedByName(actor.getUsername());
        return opportunityRepository.saveAndFlush(value);
    }
    private Opportunity locked(String id) {
        return opportunityRepository.findForUpdateById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Opportunity：" + id));
    }
    private void requireOpen(Opportunity value) {
        if (value.getClosedAt() != null || "需求成交".equals(value.getStage()) || "失單".equals(value.getStage()))
            throw new QuoteException(org.springframework.http.HttpStatus.CONFLICT, "OPPORTUNITY_CLOSED", "已結案商機不可重複結案或直接修改；結案請使用專用結案 API。");
    }
    private void validateReferences(Opportunity opportunity) {
        if (!customers.existsById(opportunity.getCustomerId())) {
            throw new IllegalArgumentException("所屬客戶不存在");
        }
        if (opportunity.getLeadId() != null && !leads.existsById(opportunity.getLeadId())) {
            throw new IllegalArgumentException("來源 Lead 不存在");
        }
    }
}
