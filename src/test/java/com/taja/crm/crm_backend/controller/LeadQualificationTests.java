package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.repo.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LeadQualificationTests extends JwtTestSupport {
    @Autowired MockMvc mvc;
    @Autowired LeadRepository leads;
    @Autowired CustomerRepository customers;
    @Autowired ContactRepository contacts;
    @Autowired OpportunityRepository opportunities;
    @Autowired EntityManager em;

    Lead seed() {
        Lead lead = new Lead();
        lead.setName("王小明"); lead.setCompany("測試公司"); lead.setEmail("lead@example.com");
        lead.setPhone("0912345678"); lead.setOwner("taja");
        return leads.saveAndFlush(lead);
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer_contact", "customer_contact_opportunity"})
    void approvalCreatesLinkedRecordsAndPreventsDuplicateConversion(String conversion) throws Exception {
        Lead lead = seed();
        long customerCount = customers.count(), contactCount = contacts.count(), opportunityCount = opportunities.count();
        String payload = "{\"decision\":\"approved\",\"conversionType\":\"" + conversion + "\",\"note\":\"符合需求\"}";
        mvc.perform(post("/api/leads/{id}/qualification", lead.getId()).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("已合格"))
                .andExpect(jsonPath("$.qualification.decision").value("approved"))
                .andExpect(jsonPath("$.qualification.reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.qualification.customerId").isNotEmpty())
                .andExpect(jsonPath("$.qualification.contactId").isNotEmpty());
        em.flush(); em.clear();
        var qualification = leads.findById(lead.getId()).orElseThrow().getQualification();
        java.time.Instant.parse(qualification.getReviewedAt());
        var customer = customers.findById(qualification.getCustomerId()).orElseThrow();
        var contact = contacts.findById(qualification.getContactId()).orElseThrow();
        assertEquals("測試公司", customer.getName());
        assertEquals("王小明", contact.getName());
        assertEquals(customer.getId(), contact.getCustomerId());
        assertEquals("lead@example.com", contact.getEmail());
        boolean withOpportunity = conversion.endsWith("opportunity");
        if (withOpportunity) {
            var opportunity = opportunities.findById(qualification.getOpportunityId()).orElseThrow();
            assertEquals(customer.getId(), opportunity.getCustomerId());
            assertEquals(contact.getId(), opportunity.getContactId());
            assertEquals(lead.getId(), opportunity.getLeadId());
            assertEquals(0, java.math.BigDecimal.ZERO.compareTo(opportunity.getAmount()));
            assertEquals("需求確認", opportunity.getStage());
        } else {
            assertNull(qualification.getOpportunityId());
        }
        mvc.perform(post("/api/leads/{id}/qualification", lead.getId()).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
        mvc.perform(put("/api/leads/{id}", lead.getId()).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"覆寫\"}"))
                .andExpect(status().isConflict());
        assertEquals(customerCount + 1, customers.count());
        assertEquals(contactCount + 1, contacts.count());
        assertEquals(opportunityCount + (withOpportunity ? 1 : 0), opportunities.count());
    }

    @Test
    void rejectionOnlyRecordsReview() throws Exception {
        Lead lead = seed();
        long customerCount = customers.count(), contactCount = contacts.count(), opportunityCount = opportunities.count();
        mvc.perform(post("/api/leads/{id}/qualification", lead.getId()).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"rejected\",\"reason\":\"預算不足\",\"note\":\"下次再聯繫\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("不合格"))
                .andExpect(jsonPath("$.qualification.reason").value("預算不足"))
                .andExpect(jsonPath("$.qualification.customerId").doesNotExist());
        assertEquals(customerCount, customers.count());
        assertEquals(contactCount, contacts.count());
        assertEquals(opportunityCount, opportunities.count());
    }

    @Test
    void invalidRequestsAndUnauthenticatedRequestsAreRejected() throws Exception {
        Lead lead = seed();
        for (String payload : new String[]{"{}", "{\"decision\":\"unknown\"}", "{\"decision\":\"approved\"}",
                "{\"decision\":\"rejected\",\"conversionType\":\"customer_contact\"}"}) {
            mvc.perform(post("/api/leads/{id}/qualification", lead.getId()).header("Authorization", bearerToken)
                    .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/leads/missing/qualification").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"rejected\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/leads/{id}/qualification", lead.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"rejected\"}"))
                .andExpect(status().isUnauthorized());
        assertNull(leads.findById(lead.getId()).orElseThrow().getQualification());
    }

    @ParameterizedTest
    @ValueSource(strings = {"已合格", "不合格"})
    void reviewedStatusAlonePreventsQualification(String reviewedStatus) throws Exception {
        Lead lead = seed();
        lead.setStatus(reviewedStatus);
        leads.saveAndFlush(lead);
        long customerCount = customers.count(), contactCount = contacts.count();
        mvc.perform(post("/api/leads/{id}/qualification", lead.getId()).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"approved\",\"conversionType\":\"customer_contact\"}"))
                .andExpect(status().isConflict());
        assertEquals(customerCount, customers.count());
        assertEquals(contactCount, contacts.count());
    }
}
