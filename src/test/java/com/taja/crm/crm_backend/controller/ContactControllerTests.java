package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContactControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void contactCrudPersistsChangesAndReturnsExpectedResponses() throws Exception {
        String response = mvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"測試聯絡人","company":"測試公司","email":"contact@example.com",
                                 "phone":"0912345678","owner":"王小明","customerId":"customer-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("測試聯絡人"))
                .andExpect(jsonPath("$.company").value("測試公司"))
                .andExpect(jsonPath("$.email").value("contact@example.com"))
                .andExpect(jsonPath("$.phone").value("0912345678"))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.owner").value("王小明"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(put("/api/contacts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"更新聯絡人","company":"新公司","email":"updated@example.com",
                                 "phone":"0987654321","owner":"李小華","customerId":"customer-2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新聯絡人"))
                .andExpect(jsonPath("$.company").value("新公司"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("0987654321"))
                .andExpect(jsonPath("$.customerId").value("customer-2"))
                .andExpect(jsonPath("$.owner").value("李小華"));

        mvc.perform(delete("/api/contacts/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingContactCannotBeUpdatedOrDeleted() throws Exception {
        String missing = "00000000-0000-0000-0000-000000000000";
        mvc.perform(put("/api/contacts/{id}", missing)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"不存在\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("找不到 Contact：" + missing));
        mvc.perform(delete("/api/contacts/{id}", missing))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
