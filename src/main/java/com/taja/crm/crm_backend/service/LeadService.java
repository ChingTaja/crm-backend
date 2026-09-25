package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.repo.LeadRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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

    public List<Lead> findAllLeads() {
        return leadRepository.findAll();
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

    /** 完整更新指定 Lead；省略 qualification 時會清除原本的審核資料。 */
    @Transactional
    public Lead updateLeads(String id, @NotNull @Valid Lead lead) {
        Lead existing = findByIdLead(id);
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
