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
class OrderControllerTests extends JwtTestSupport {
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
        else user.setRole(businessRole);
        users.saveAndFlush(user);
        return "Bearer "+jwt.issue(UserResponse.fromEntity(user)).accessToken();
    }
    String convert() throws Exception {
        create("10.01","10");
        action("send","{\"expectedRevision\":1}").andExpect(status().isOk());
        action("decision","{\"expectedRevision\":2,\"decision\":\"accepted\"}").andExpect(status().isOk());
        String json=action("convert-to-order","{\"expectedRevision\":3}").andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json,"$.orderId");
    }
    ResultActions change(String order,String status,int revision,String reason) throws Exception {
        return mvc.perform(patch("/api/orders/"+order+"/status").header("Authorization",bearerToken)
            .contentType(MediaType.APPLICATION_JSON).content("""
            {"status":"%s","expectedRevision":%d,"reason":"%s"}
            """.formatted(status,revision,reason)));
    }
    @Test void immutableSnapshotsAndStateMachine() throws Exception {
        String order=convert();
        customer.setName("改名");customers.saveAndFlush(customer);
        product.setName("改產品");product.setPrice(new BigDecimal("999"));products.saveAndFlush(product);
        mvc.perform(get("/api/orders/"+order).header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.customerName").value("客戶"))
            .andExpect(jsonPath("$.lines[0].productName").value("原產品"))
            .andExpect(jsonPath("$.lines[0].totalCents").value(2838))
            .andExpect(jsonPath("$.totals.totalCents").value(2838))
            .andExpect(jsonPath("$.revision").value(1))
            .andExpect(jsonPath("$.status").value("Confirmed"))
            .andExpect(jsonPath("$.audit[0].action").value("CreatedFromQuote"))
            .andExpect(jsonPath("$.allowedTransitions.length()").value(2));
        change(order,"Completed",1,"").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"));
        change(order,"Cancelled",1," ").andExpect(status().isBadRequest());
        change(order,"Processing",1,"").andExpect(status().isOk())
            .andExpect(jsonPath("$.revision").value(2)).andExpect(jsonPath("$.processingAt").isNotEmpty());
        change(order,"Cancelled",1,"取消").andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORDER_REVISION_CONFLICT"));
        change(order,"Completed",2,"").andExpect(status().isOk())
            .andExpect(jsonPath("$.revision").value(3)).andExpect(jsonPath("$.completedAt").isNotEmpty())
            .andExpect(jsonPath("$.audit[2].fromStatus").value("Processing"))
            .andExpect(jsonPath("$.allowedTransitions").isEmpty());
        change(order,"Cancelled",3,"取消").andExpect(status().isConflict());
    }
    @Test void cancellationPermissionsAndServerSideFilters() throws Exception {
        String order=convert();
        mvc.perform(get("/api/orders").header("Authorization",bearerToken).param("keyword","年度")
            .param("customerId",customer.getId()).param("status","Confirmed").param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].lines").doesNotExist());
        change(order,"Cancelled",1," 客戶取消 ").andExpect(status().isOk())
            .andExpect(jsonPath("$.cancellationReason").value("客戶取消"))
            .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
            .andExpect(jsonPath("$.audit[1].actorId").isNotEmpty())
            .andExpect(jsonPath("$.allowedTransitions").isEmpty());
        mvc.perform(get("/api/orders").header("Authorization",bearerToken).param("status","Confirmed"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        bearerToken=actor(false);
        mvc.perform(get("/api/orders/"+order).header("Authorization",bearerToken)).andExpect(status().isForbidden());
        change(order,"Processing",2,"").andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        bearerToken=actor(true);
        mvc.perform(get("/api/orders/"+order).header("Authorization",bearerToken)).andExpect(status().isOk());
    }
    @Test void jwtValidationMetadataAndNoMutationEndpoints() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/unknown")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/orders/unknown/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entities/orders/fields").header("Authorization",bearerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$[7].options[0].key").value("Confirmed"));
        mvc.perform(get("/api/orders").param("sort","passwordHash").header("Authorization",bearerToken))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").param("createdFrom","2026-12-01").param("createdTo","2026-01-01")
            .header("Authorization",bearerToken)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").param("status","unknown").header("Authorization",bearerToken))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").header("Authorization",bearerToken)).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/orders/unknown").header("Authorization",bearerToken)).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/orders/unknown").header("Authorization",bearerToken)).andExpect(status().isMethodNotAllowed());
    }
    @Test void allowedTransitionsUsesCurrentPermissions() throws Exception {
        String order=convert();
        businessRole.getPermissionCodes().remove("orders.cancel");roles.saveAndFlush(businessRole);
        mvc.perform(get("/api/orders/"+order).header("Authorization",bearerToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.allowedTransitions.length()").value(1))
            .andExpect(jsonPath("$.allowedTransitions[0]").value("Processing"));
        change(order,"Cancelled",1,"取消").andExpect(status().isForbidden());
        change(order,"Processing",1,"").andExpect(status().isOk());
    }
}
