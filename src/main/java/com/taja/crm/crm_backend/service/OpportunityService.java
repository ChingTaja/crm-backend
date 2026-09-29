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
        validateReferences(opportunity);
        return opportunityRepository.save(opportunity);
    }

    /** 完整更新指定 Opportunity，未提供的欄位會清空。 */
    @Transactional
    public Opportunity updateOpportunities(String id, @NotNull @Valid Opportunity opportunity) {
        Opportunity existing = findByIdOpportunity(id);
        validateReferences(opportunity);
        existing.setName(opportunity.getName());
        existing.setCustomerId(opportunity.getCustomerId());
        existing.setLeadId(opportunity.getLeadId());
        existing.setAmount(opportunity.getAmount());
        existing.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        existing.setOwner(opportunity.getOwner());
        existing.setStage(opportunity.getStage());
        return opportunityRepository.save(existing);
    }

    @Transactional
    public void deleteOpportunities(String id) {
        Opportunity opportunity = findByIdOpportunity(id);
        opportunityRepository.delete(opportunity);
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
