package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.lead.QualifyLeadRequest;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.model.Opportunity;
import com.taja.crm.crm_backend.repo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class LeadQualificationRollbackTests {
    @Autowired LeadQualificationService service;
    @Autowired LeadRepository leads;
    @Autowired CustomerRepository customers;
    @Autowired ContactRepository contacts;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean OpportunityRepository opportunities;

    @Test
    void opportunityFailureRollsBackCustomerContactAndReview() {
        long leadCount = leads.count(), customerCount = customers.count(), contactCount = contacts.count();
        doThrow(new IllegalStateException("simulated persistence failure"))
                .when(opportunities).save(any(Opportunity.class));
        var transaction = new TransactionTemplate(transactions);
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            Lead lead = new Lead(); lead.setName("回滾測試");
            leads.saveAndFlush(lead);
            return service.qualifyLead(lead.getId(),
                    new QualifyLeadRequest("approved", "customer_contact_opportunity", null, null));
        }));
        assertEquals(leadCount, leads.count());
        assertEquals(customerCount, customers.count());
        assertEquals(contactCount, contacts.count());
    }
}
