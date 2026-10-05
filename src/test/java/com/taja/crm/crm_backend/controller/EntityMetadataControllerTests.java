package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.service.EntityMetadataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(EntityMetadataController.class)
@Import(EntityMetadataService.class)
class EntityMetadataControllerTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.taja.crm.crm_backend.service.PermissionService permissions;
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"leads", "customers", "contacts"})
    void exposesCommonFieldsAndReadOnlyId(String entity) throws Exception {
        mvc.perform(get("/api/entities/{entity}/fields", entity))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("id"))
                .andExpect(jsonPath("$[0].readOnly").value(true))
                .andExpect(jsonPath("$[1].type").value("string"))
                .andExpect(jsonPath("$[1].displayName").isNotEmpty())
                .andExpect(jsonPath("$[1].options").doesNotExist())
                .andExpect(jsonPath("$[1].relatedEntityName").doesNotExist());
    }

    @Test
    void leadIncludesOptionsAndEmbeddedDatabaseColumnNames() throws Exception {
        mvc.perform(get("/api/entities/leads/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'status')].type", hasItem("optionSet")))
                .andExpect(jsonPath("$[?(@.name == 'status')].options[0].key", hasItem("pending")))
                .andExpect(jsonPath("$[?(@.name == 'status')].options[0].value", hasItem("待聯繫")))
                .andExpect(jsonPath("$[?(@.name == 'qualification_decision')].options[0].key", hasItem("approved")))
                .andExpect(jsonPath("$[?(@.name == 'qualification_reviewed_at')].apiFieldName", hasItem("qualification.reviewedAt")))
                .andExpect(jsonPath("$[?(@.name == 'qualification_contact_id')].relatedEntityName", hasItem("contacts")));
    }

    @Test
    void contactLookupIdentifiesCustomerAndReferencedField() throws Exception {
        mvc.perform(get("/api/entities/contacts/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'customer_id')].type", hasItem("lookup")))
                .andExpect(jsonPath("$[?(@.name == 'customer_id')].apiFieldName", hasItem("customerId")))
                .andExpect(jsonPath("$[?(@.name == 'customer_id')].relatedEntityName", hasItem("customers")))
                .andExpect(jsonPath("$[?(@.name == 'customer_id')].relatedFiledName", hasItem("id")));
    }

    @Test
    void unknownEntityReturnsNotFound() throws Exception {
        mvc.perform(get("/api/entities/unknown/fields"))
                .andExpect(status().isNotFound());
    }
}
