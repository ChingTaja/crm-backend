package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
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
class CustomerControllerTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void customerCrudPersistsChangesAndReturnsExpectedResponses() throws Exception {
        String response = mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"測試客戶","company":"測試公司","email":"customer@example.com",
                                 "phone":"0912345678","owner":"王小明"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("測試客戶"))
                .andExpect(jsonPath("$.company").value("測試公司"))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.phone").value("0912345678"))
                .andExpect(jsonPath("$.owner").value("王小明"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");

        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        mvc.perform(put("/api/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"更新客戶","company":"新公司","email":"updated@example.com",
                                 "phone":"0987654321","owner":"李小華"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新客戶"))
                .andExpect(jsonPath("$.company").value("新公司"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("0987654321"))
                .andExpect(jsonPath("$.owner").value("李小華"));

        mvc.perform(delete("/api/customers/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingCustomerCannotBeUpdatedOrDeleted() throws Exception {
        String missing = "00000000-0000-0000-0000-000000000000";
        mvc.perform(put("/api/customers/{id}", missing)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"不存在\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("找不到 Customer：" + missing));
        mvc.perform(delete("/api/customers/{id}", missing))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedRequestReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
