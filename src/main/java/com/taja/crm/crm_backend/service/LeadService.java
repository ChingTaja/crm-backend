package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.repo.LeadRepository;
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
public class LeadService {

    private final LeadRepository leadRepository;

    public Page<Lead> findAllLeads(Pageable pageable) {
        return leadRepository.findAll(pageable);
    }

    public Lead findByIdLead(String id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Lead：" + id));
    }

    @Transactional
    public Lead createLeads(@NotNull @Valid Lead lead) {
        if (lead.getId() != null) {
            throw new IllegalArgumentException("新增 Lead 時不可指定 id");
        }
        return leadRepository.save(lead);
    }

    /** 完整更新尚未審核的 Lead；已完成審核的資料不可覆寫。 */
    @Transactional
    public Lead updateLeads(String id, @NotNull @Valid Lead lead) {
        Lead existing = leadRepository.findForUpdateById(id)
                .orElseThrow(() -> new EntityNotFoundException("找不到 Lead：" + id));
        if (existing.getQualification() != null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "已完成審核的 Lead 不可透過一般更新覆寫");
        }
        existing.setName(lead.getName());
        existing.setCompany(lead.getCompany());
        existing.setEmail(lead.getEmail());
        existing.setPhone(lead.getPhone());
        existing.setSource(lead.getSource());
        existing.setOwner(lead.getOwner());
        existing.setStatus(lead.getStatus());
        existing.setQualification(lead.getQualification());
        return leadRepository.save(existing);
    }

    @Transactional
    public void deleteLeads(String id) {
        Lead lead = findByIdLead(id);
        leadRepository.delete(lead);
    }
}
