package com.taja.crm.crm_backend.controller;

import com.jayway.jsonpath.JsonPath;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.dto.auth.UserResponse;
import com.taja.crm.crm_backend.service.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QuoteControllerTests extends JwtTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SalesOrderRepository orders;
    @Autowired QuoteRepository quotes;
    @Autowired JwtService jwt;
    Product product;
    Customer customer;
    String id, versionId;
    @BeforeEach void fixtures() {
        product = new Product(); product.setName("原產品"); product.setSku(UUID.randomUUID().toString());
        product.setPrice(new BigDecimal("100")); product.setStatus("啟用"); products.saveAndFlush(product);
        customer = new Customer(); customer.setName("客戶"); customers.saveAndFlush(customer);
    }
    String body(String price, String discount) {
        return """
            {"name":"年度報價","customerId":"%s","validUntil":"%s","paymentTerms":"月結",
            "lines":[{"productId":"%s","quantity":3,"unitPrice":%s,"discountPercent":%s,"taxPercent":5}]}
            """.formatted(customer.getId(), LocalDate.now().plusDays(30), product.getId(), price, discount);
    }
    String create(String price, String discount) throws Exception {
        String json = mvc.perform(post("/api/quotes").header("Authorization",bearerToken)
            .contentType(MediaType.APPLICATION_JSON).content(body(price,discount)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        id = JsonPath.read(json,"$.id"); versionId = JsonPath.read(json,"$.versions[0].id"); return json;
    }
    ResultActions action(String action, String json) throws Exception {
        return mvc.perform(post("/api/quotes/{id}/versions/{v}/{action}",id,versionId,action)
            .header("Authorization",bearerToken).contentType(MediaType.APPLICATION_JSON).content(json));
    }
    String actor(boolean admin) {
        User user = new User(); user.setUsername("quote-test-"+UUID.randomUUID());
        user.setEmail(user.getUsername()+"@example.com"); user.setPasswordHash("unused");
        if (admin) user.setRole(roles.findByCode("ADMIN").orElseThrow());
        users.saveAndFlush(user);
        return "Bearer "+jwt.issue(UserResponse.fromEntity(user)).accessToken();
    }
    @Test void moneyConversionSnapshotsAndRetry() throws Exception {
        String json=create("10.01","10");
        assertEquals(3003, (int)JsonPath.read(json,"$.versions[0].totals.subtotalCents"));
        assertEquals(300, (int)JsonPath.read(json,"$.versions[0].totals.discountCents"));
        assertEquals(135, (int)JsonPath.read(json,"$.versions[0].totals.taxCents"));
        assertEquals(2838, (int)JsonPath.read(json,"$.versions[0].totals.totalCents"));
        action("send","{\"expectedRevision\":1}").andExpect(status().isOk());
        action("decision","{\"expectedRevision\":2,\"decision\":\"accepted\"}").andExpect(status().isOk());
        product.setName("新名稱"); product.setPrice(new BigDecimal("900")); products.saveAndFlush(product);
        String converted=action("convert-to-order","{\"expectedRevision\":3}").andExpect(status().isOk())
            .andExpect(jsonPath("$.quote.versions[0].revision").value(4))
            .andReturn().getResponse().getContentAsString();
        String orderId=JsonPath.read(converted,"$.orderId");
        action("convert-to-order","{\"expectedRevision\":3}").andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId));
        SalesOrder order=orders.findById(orderId).orElseThrow();
        assertEquals("已確認",order.getStatus()); assertEquals(2838,order.getTotals().getTotalCents());
        assertEquals("原產品",order.getLines().getFirst().getProductName());
        assertEquals(0,new BigDecimal("100").compareTo(order.getLines().getFirst().getCatalogPrice()));
        assertEquals("月結",order.getTerms().getPaymentTerms());
        mvc.perform(delete("/api/quotes/"+id).header("Authorization",bearerToken)).andExpect(status().isConflict());
    }
    @Test void approvalsVersionsAndHistoricalReadOnly() throws Exception {
        create("100","11");
        action("send","{\"expectedRevision\":1}").andExpect(status().isConflict());
        action("request-approval","{\"expectedRevision\":1}").andExpect(status().isOk())
            .andExpect(jsonPath("$.versions[0].approval").value("Pending"));
        action("review","{\"expectedRevision\":2,\"decision\":\"approved\"}").andExpect(status().isForbidden());
        String owner=bearerToken; bearerToken=actor(true);
        action("review","{\"expectedRevision\":2,\"decision\":\"rejected\"}").andExpect(status().isBadRequest());
        action("review","{\"expectedRevision\":2,\"decision\":\"approved\"}").andExpect(status().isOk());
        bearerToken=owner;
        mvc.perform(put("/api/quotes/"+id+"/versions/"+versionId).header("Authorization",bearerToken)
            .contentType(MediaType.APPLICATION_JSON).content(body("100","11").replace("{\"name\"", "{\"expectedRevision\":3,\"name\"")))
            .andExpect(status().isConflict());
        product.setPrice(new BigDecimal("200")); products.saveAndFlush(product);
        action("new-version","{\"expectedRevision\":3}").andExpect(status().isOk())
            .andExpect(jsonPath("$.versions[1].version").value(2))
            .andExpect(jsonPath("$.versions[1].requiresReapproval").value(true))
            .andExpect(jsonPath("$.versions[1].approval").value("Required"))
            .andExpect(jsonPath("$.versions[1].lines[0].catalogPrice").value(100));
        action("send","{\"expectedRevision\":4}").andExpect(status().isConflict());
    }
    @Test void revisionValidationOwnershipAndPagination() throws Exception {
        create("100","0");
        action("send","{\"expectedRevision\":2}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("QUOTE_REVISION_CONFLICT"));
        mvc.perform(get("/api/quotes").header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].versions").doesNotExist());
        bearerToken=actor(false);
        mvc.perform(get("/api/quotes/"+id).header("Authorization",bearerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/quotes").header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/quotes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entities/quotes/fields")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entities/quotes/fields").header("Authorization",bearerToken)).andExpect(status().isOk());
    }
    @Test void updatePreservesSnapshotsAndRejectsForeignLineIds() throws Exception {
        String json=create("100","0"); String line=JsonPath.read(json,"$.versions[0].lines[0].id");
        product.setName("變更名稱"); products.saveAndFlush(product);
        String update=body("101","0").replace("{\"name\"","{\"expectedRevision\":1,\"name\"")
            .replace("{\"productId\"","{\"id\":\""+line+"\",\"productId\"");
        mvc.perform(put("/api/quotes/"+id+"/versions/"+versionId).header("Authorization",bearerToken)
            .contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versions[0].revision").value(2))
            .andExpect(jsonPath("$.versions[0].lines[0].productName").value("原產品"));
        mvc.perform(put("/api/quotes/"+id+"/versions/"+versionId).header("Authorization",bearerToken)
            .contentType(MediaType.APPLICATION_JSON).content(update.replace("expectedRevision\":1","expectedRevision\":2").replace(line,"foreign")))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/quotes/"+id).header("Authorization",bearerToken)).andExpect(status().isNoContent());
    }
    @Test void expiredAcceptanceAndRequiredThreshold() throws Exception {
        String json=create("40000","0");
        assertEquals("Required",JsonPath.read(json,"$.versions[0].approval"));
        create("100","0"); action("send","{\"expectedRevision\":1}").andExpect(status().isOk());
        Quote quote=quotes.findById(id).orElseThrow();
        quote.getVersions().getFirst().getTerms().setValidUntil(LocalDate.now().minusDays(1)); quotes.flush();
        mvc.perform(get("/api/quotes/"+id).header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.versions[0].status").value("Expired"));
        action("decision","{\"expectedRevision\":2,\"decision\":\"accepted\"}").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));
    }
}
