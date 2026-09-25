package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.service.LeadService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeadController.class)
class LeadControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LeadService leadService;

    @Test
    void createReturnsCreatedLeadWithDefaultStatusOption() throws Exception {
        Lead created = new Lead();
        created.setId("lead-1");
        created.setName("王小明");
        when(leadService.createLeads(any(Lead.class))).thenReturn(created);

        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"王小明\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("lead-1"))
                .andExpect(jsonPath("$.status.key").value("pending"))
                .andExpect(jsonPath("$.status.value").value("待聯繫"))
                .andExpect(jsonPath("$.qualification").doesNotExist());
        verify(leadService).createLeads(argThat(lead -> lead.getId() == null
                && "待聯繫".equals(lead.getStatus()) && "王小明".equals(lead.getName())));
    }

    @Test
    void invalidStatusIsRejectedBeforeCallingService() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":{\"key\":\"invalid\",\"value\":\"無效\"}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(leadService);
    }

    @Test
    void missingLeadReturnsNotFound() throws Exception {
        when(leadService.findByIdLead("missing"))
                .thenThrow(new EntityNotFoundException("找不到 Lead：missing"));

        mvc.perform(get("/api/leads/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("找不到 Lead：missing"));
    }

    @Test
    void createMapsStatusKeyAndQualification() throws Exception {
        when(leadService.createLeads(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    lead.setId("lead-2");
                    return lead;
                });

        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"王小明", "status":{"key":"qualified","value":"已合格"},
                                 "qualification":{"decision":"approved",
                                  "reviewedAt":"2026-09-22T10:00:00+08:00", "customerId":"customer-1"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status.key").value("qualified"))
                .andExpect(jsonPath("$.status.value").value("已合格"))
                .andExpect(jsonPath("$.qualification.decision").value("approved"))
                .andExpect(jsonPath("$.qualification.customerId").value("customer-1"));
        verify(leadService).createLeads(argThat(lead -> "已合格".equals(lead.getStatus())));
    }

    @Test
    void invalidQualificationIsRejectedBeforeCallingService() throws Exception {
        mvc.perform(post("/api/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qualification\":{\"decision\":\"invalid\"}}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(leadService);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/leads/lead-1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(leadService).deleteLeads("lead-1");
    }
}
