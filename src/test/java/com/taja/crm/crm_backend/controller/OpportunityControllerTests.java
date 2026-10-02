package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.repo.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpportunityControllerTests extends JwtTestSupport {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @Autowired EntityManager em;
    String customerId;

    @BeforeEach
    void seedCustomer() {
        Customer customer = new Customer(); customer.setName("商機測試客戶");
        customerId = customers.saveAndFlush(customer).getId();
    }

    String payload(String extra) {
        return "{\"name\":\"年度合作方案\",\"customerId\":\"" + customerId + "\"," + extra + "}";
    }

    @Test
    void crudUsesPaginationAndFullResponse() throws Exception {
        String json = mvc.perform(post("/api/opportunities").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload("\"amount\":150000.25,\"expectedCloseDate\":\"2026-12-31\",\"owner\":\"王小明\"")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stage").value("需求確認"))
                .andExpect(jsonPath("$.amount").value(150000.25))
                .andExpect(jsonPath("$.expectedCloseDate").value("2026-12-31"))
                .andExpect(jsonPath("$.contactId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        em.flush(); em.clear();
        mvc.perform(get("/api/opportunities/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(150000.25));
        mvc.perform(get("/api/opportunities").param("page", "0").param("size", "1").header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.totalElements").isNumber());
        mvc.perform(put("/api/opportunities/{id}", id).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(payload("\"amount\":0,\"stage\":\"已成交\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.stage").value("已成交"))
                .andExpect(jsonPath("$.owner").isEmpty()).andExpect(jsonPath("$.expectedCloseDate").isEmpty());
        em.flush(); em.clear();
        mvc.perform(delete("/api/opportunities/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/opportunities/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFieldsAndReferencesReturnBadRequest() throws Exception {
        for (String extra : new String[]{"\"amount\":-1", "\"amount\":null", "\"stage\":\"需求確認\"",
                "\"amount\":1,\"stage\":\"未知\"", "\"amount\":1,\"stage\":null",
                "\"amount\":1,\"expectedCloseDate\":\"not-a-date\"", "\"amount\":1,\"leadId\":\"missing\""}) {
            mvc.perform(post("/api/opportunities").header("Authorization", bearerToken)
                    .contentType(MediaType.APPLICATION_JSON).content(payload(extra))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/opportunities").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\",\"customerId\":\"missing\",\"amount\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/opportunities").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void metadataExposesFieldsStagesAndLookups() throws Exception {
        mvc.perform(get("/api/entities/opportunities/fields").header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[2].relatedEntityName").value("customers"))
                .andExpect(jsonPath("$[3].relatedEntityName").value("leads"))
                .andExpect(jsonPath("$[5].apiFieldName").value("expectedCloseDate"))
                .andExpect(jsonPath("$[7].options.length()").value(5))
                .andExpect(jsonPath("$[7].options[0].key").value("需求確認"));
        mvc.perform(get("/api/opportunities")).andExpect(status().isUnauthorized());
    }
}
