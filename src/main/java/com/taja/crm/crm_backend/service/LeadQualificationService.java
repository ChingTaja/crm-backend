package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.lead.QualifyLeadRequest;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LeadQualificationService {
    private final LeadRepository leads;
    private final CustomerRepository customers;
    private final ContactRepository contacts;
    private final OpportunityRepository opportunities;
    private final Clock clock;

    @Transactional
    public Lead qualifyLead(String id, QualifyLeadRequest request) {
        boolean approved = "approved".equals(request.decision());
        if (!approved && !"rejected".equals(request.decision())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的審核結果");
        }
        if (approved && !"customer_contact".equals(request.conversionType())
                && !"customer_contact_opportunity".equals(request.conversionType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通過審核時必須選擇建立資料的方式");
        }
        if (!approved && request.conversionType() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不符合資格時不可指定轉換方式");
        }
        Lead lead = leads.findForUpdateById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到 Lead：" + id));
        if (lead.getQualification() != null || "已合格".equals(lead.getStatus()) || "不合格".equals(lead.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此 Lead 已完成審核，請重新取得最新資料");
        }
        LeadQualification qualification = new LeadQualification();
        qualification.setDecision(request.decision());
        qualification.setReviewedAt(clock.instant().toString());
        qualification.setReason(request.reason());
        qualification.setNote(request.note());
        if (approved) {
            Customer customer = new Customer();
            customer.setName(lead.getCompany() == null || lead.getCompany().isBlank() ? lead.getName() : lead.getCompany());
            customer.setCompany(lead.getCompany());
            customer.setEmail(lead.getEmail());
            customer.setPhone(lead.getPhone());
            customer.setOwner(lead.getOwner());
            customers.save(customer);

            Contact contact = new Contact();
            contact.setName(lead.getName());
            contact.setCompany(lead.getCompany());
            contact.setEmail(lead.getEmail());
            contact.setPhone(lead.getPhone());
            contact.setOwner(lead.getOwner());
            contact.setCustomerId(customer.getId());
            contacts.save(contact);
            qualification.setCustomerId(customer.getId());
            qualification.setContactId(contact.getId());

            if ("customer_contact_opportunity".equals(request.conversionType())) {
                Opportunity opportunity = new Opportunity();
                opportunity.setName(customer.getName() + " 商機");
                opportunity.setOwner(lead.getOwner());
                opportunity.setCustomerId(customer.getId());
                opportunity.setContactId(contact.getId());
                opportunity.setLeadId(lead.getId());
                opportunities.save(opportunity);
                qualification.setOpportunityId(opportunity.getId());
            }
        }
        lead.setStatus(approved ? "已合格" : "不合格");
        lead.setQualification(qualification);
        return leads.saveAndFlush(lead);
    }
}
