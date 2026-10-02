package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTests extends JwtTestSupport {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    ResultActions create(String body) throws Exception {
        return mvc.perform(post("/api/products").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void crudNormalizesFieldsAndSupportsCaseInsensitiveUniqueSku() throws Exception {
        String sku = "SKU-" + UUID.randomUUID();
        String body = "{\"name\":\" 年度服務方案 \",\"sku\":\" " + sku + " \",\"price\":12000.5}";
        String json = create(body).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("年度服務方案"))
                .andExpect(jsonPath("$.sku").value(sku)).andExpect(jsonPath("$.status").value("啟用"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        create(body.replace(sku, sku.toLowerCase())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("產品編號已存在。"));
        mvc.perform(get("/api/products/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(12000.5));
        mvc.perform(get("/api/products").param("size", "1").header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1));
        mvc.perform(put("/api/products/{id}", id).header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新版服務\",\"sku\":\"" + sku.toLowerCase() + "\",\"price\":0,\"status\":\"停用\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("停用")).andExpect(jsonPath("$.price").value(0));
        mvc.perform(delete("/api/products/{id}", id).header("Authorization", bearerToken)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", id).header("Authorization", bearerToken)).andExpect(status().isNotFound());
    }

    @Test
    void invalidProductFieldsAreRejected() throws Exception {
        for (String body : new String[]{"{}", "{\"name\":\" \",\"sku\":\"x\",\"price\":0}",
                "{\"name\":\"x\",\"sku\":\" \",\"price\":0}", "{\"name\":\"x\",\"sku\":\"x\",\"price\":-1}",
                "{\"name\":\"x\",\"sku\":\"x\",\"price\":1.001}",
                "{\"name\":\"x\",\"sku\":\"x\",\"price\":1,\"status\":\"unknown\"}"}) {
            create(body).andExpect(status().isBadRequest());
        }
    }

    @Test
    void productSearchAndMetadataFollowContract() throws Exception {
        String sku = "SKU-" + UUID.randomUUID();
        create("{\"name\":\"年度服務方案\",\"sku\":\"" + sku + "\",\"price\":12000.5}")
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products/search").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"page":0,"size":20,"keyword":"%s","filter":{"kind":"group","match":"all","children":[
                {"kind":"rule","field":"price","operator":"greaterThanOrEqual","value":12000},
                {"kind":"rule","field":"status","operator":"equals","value":"啟用"}]}}
                """.formatted(sku)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(post("/api/products/search").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":0,\"size\":20,\"filter\":{\"kind\":\"rule\",\"field\":\"status\",\"operator\":\"equals\",\"value\":\"unknown\"}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/entities/products/fields").header("Authorization", bearerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].type").value("option"))
                .andExpect(jsonPath("$[3].options[0].key").value("啟用"));
    }

    @Test
    void allProductRoutesRequireJwt() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products/missing")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/products/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/products/missing")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entities/products/fields")).andExpect(status().isUnauthorized());
    }

    @Test
    void databaseIndexRejectsDuplicateSkuEvenWithoutServiceCheck() throws Exception {
        String sku = "INDEX-" + UUID.randomUUID();
        create("{\"name\":\"test\",\"sku\":\"" + sku + "\",\"price\":1}")
                .andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DuplicateKeyException.class,
                () -> jdbc.update("INSERT INTO products (id,name,sku,price,status) VALUES (?,?,?,?,?)",
                        UUID.randomUUID().toString(), "duplicate", sku.toLowerCase(), 1, "啟用"));
    }

    @Test
    void referencedProductDeletionReturnsConflict() throws Exception {
        String sku = "REF-" + UUID.randomUUID();
        String response = create("{\"name\":\"test\",\"sku\":\"" + sku + "\",\"price\":1}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.id");
        // PostgreSQL temporary tables cannot reference persistent tables.
        // DDL participates in this test transaction and is rolled back after the test.
        String table = "product_reference_test_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE TABLE " + table + " (product_id varchar(255) REFERENCES products(id))");
        jdbc.update("INSERT INTO " + table + " VALUES (?)", id);
        mvc.perform(delete("/api/products/{id}", id).header("Authorization", bearerToken))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("產品已被引用，請改用停用。"));
    }
}
