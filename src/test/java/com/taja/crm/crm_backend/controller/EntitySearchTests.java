package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.dto.auth.UserResponse;
import com.taja.crm.crm_backend.service.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EntitySearchTests extends JwtTestSupport {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OpportunityRepository opportunities;
    @Autowired CustomerRepository customers;
    @Autowired LeadRepository leads;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired JwtService jwt;
    String marker;
    Customer customer;

    @BeforeEach
    void seed() {
        marker = "search-" + UUID.randomUUID();
        customer = new Customer(); customer.setName(marker); customers.saveAndFlush(customer);
    }

    Map<String,Object> rule(String field, String op, Object value) {
        Map<String,Object> rule = new LinkedHashMap<>();
        rule.put("kind", "rule"); rule.put("field", field); rule.put("operator", op);
        if (value != null) rule.put("value", value);
        return rule;
    }
    Map<String,Object> group(String match, Object... nodes) {
        return Map.of("kind", "group", "match", match, "children", Arrays.asList(nodes));
    }
    Map<String,Object> request(Object filter) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("page", 0); body.put("size", 20); body.put("filter", filter);
        return body;
    }
    ResultActions search(String entity, Map<String,Object> body) throws Exception {
        return mvc.perform(post("/api/" + entity + "/search").header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    Opportunity opportunity(String suffix, String amount, String stage, String date) {
        Opportunity value = new Opportunity(); value.setName(marker + suffix); value.setCustomerId(customer.getId());
        value.setAmount(new BigDecimal(amount)); value.setStage(stage);
        value.setExpectedCloseDate(date == null ? null : LocalDate.parse(date));
        return opportunities.saveAndFlush(value);
    }

    @Test
    void nestedFiltersKeywordNumericDateAndStablePagination() throws Exception {
        var first = opportunity(" 年度 A", "150000", "需求確認", "2026-12-31");
        var second = opportunity(" 年度 B", "150000", "提案報價", "2026-12-01");
        opportunity(" 年度 excluded stage", "200000", "已失單", "2026-12-01");
        opportunity(" 年度 excluded date", "200000", "需求確認", null);
        opportunity(" unrelated keyword", "200000", "需求確認", "2026-12-01");
        var body = request(group("all", rule("customerId", "equals", customer.getId()),
                rule("amount", "greaterThanOrEqual", 100000),
                group("any", rule("stage", "equals", "需求確認"), rule("stage", "equals", "提案報價")),
                rule("expectedCloseDate", "lessThanOrEqual", "2026-12-31")));
        body.put("keyword", " 年度 "); body.put("size", 1);
        body.put("sort", List.of(Map.of("field", "amount", "direction", "desc")));
        List<String> ids = new ArrayList<>(List.of(first.getId(), second.getId())); Collections.sort(ids);
        search("opportunities", body).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].id").value(ids.getFirst()));
        body.put("page", 1);
        search("opportunities", body).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(ids.getLast()));
        body.put("page", 3);
        search("opportunities", body).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void textIgnoresCaseAndWhitespaceAndEscapesLikeWildcards() throws Exception {
        customer.setCompany(" \tMiXeD 100%_Deal\n "); customers.saveAndFlush(customer);
        for (var condition : List.of(rule("company", "equals", " mixed 100%_deal "),
                rule("company", "contains", "100%_"), rule("company", "startsWith", " MIXED"),
                rule("company", "notContains", "absent"), rule("company", "notEquals", "other"))) {
            search("customers", request(group("all", rule("id", "equals", customer.getId()), condition)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        }
        customer.setCompany("100XXDeal"); customers.saveAndFlush(customer);
        search("customers", request(group("all", rule("id", "equals", customer.getId()), rule("company", "contains", "%_"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void emptyAndNotEmptyHandleNullAndWhitespace() throws Exception {
        for (String value : new String[]{null, "", " \t\r\n "}) {
            customer.setCompany(value); customers.saveAndFlush(customer);
            search("customers", request(group("all", rule("id", "equals", customer.getId()), rule("company", "empty", null))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
            search("customers", request(group("all", rule("id", "equals", customer.getId()), rule("company", "notEmpty", null))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void leadOptionKeysAndNestedQualificationDatesAreMapped() throws Exception {
        Lead lead = new Lead(); lead.setName(marker); lead.setStatus("已合格");
        LeadQualification qualification = new LeadQualification(); qualification.setDecision("approved");
        qualification.setReviewedAt("2026-09-30T15:20:00Z"); lead.setQualification(qualification); leads.saveAndFlush(lead);
        search("leads", request(group("all", rule("id", "equals", lead.getId()), rule("status", "equals", "qualified"),
                rule("qualification.decision", "equals", "approved"), rule("qualification.reviewedAt", "equals", "2026-09-30"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        search("leads", request(rule("status", "equals", "已合格"))).andExpect(status().isBadRequest());
    }

    @Test
    void allPublicBusinessEntitiesSupportEmptyRootAndKeyword() throws Exception {
        for (String entity : List.of("leads", "customers", "contacts", "opportunities", "roles")) {
            var none = request(null); none.put("keyword", marker);
            var empty = request(group("any")); empty.put("keyword", marker);
            String one = search(entity, none).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            String two = search(entity, empty).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertEquals(json.readTree(one), json.readTree(two));
        }
    }

    @Test
    void permissionsAndSensitiveFieldsCannotBeBypassed() throws Exception {
        mvc.perform(post("/api/customers/search").contentType(MediaType.APPLICATION_JSON).content("{\"page\":0,\"size\":20}"))
                .andExpect(status().isUnauthorized());
        search("users", request(group("any"))).andExpect(status().isForbidden());
        Role adminRole = roles.findByCode("ADMIN").orElseGet(() -> {
            Role r = new Role(); r.setCode("ADMIN"); r.setName("管理員"); return roles.saveAndFlush(r);
        });
        User admin = new User(); admin.setUsername(marker); admin.setEmail(marker + "@example.com");
        admin.setPasswordHash("not-returned"); admin.setRole(adminRole); users.saveAndFlush(admin);
        bearerToken = "Bearer " + jwt.issue(UserResponse.fromEntity(admin)).accessToken();
        search("users", request(group("all", rule("username", "equals", marker), rule("roleId", "equals", adminRole.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        search("users", request(rule("passwordHash", "equals", "secret"))).andExpect(status().isBadRequest());
        search("password_reset_tokens", request(null)).andExpect(status().isNotFound());
    }

    @Test
    void invalidFieldsOperatorsTypesAndLimitsReturnProblemDetail() throws Exception {
        search("opportunities", request(rule("amount", "contains", "1")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("amount 不支援 contains"));
        for (Object filter : List.of(rule("金額", "equals", 1), rule("name; DROP TABLE users", "equals", "x"),
                rule("amount", "equals", "100"), rule("expectedCloseDate", "equals", "2026-02-30"),
                rule("stage", "equals", "unknown"), rule("name", "empty", ""), group("all", group("any")))) {
            search("opportunities", request(filter)).andExpect(status().isBadRequest());
        }
        Object deep = rule("name", "equals", "x");
        for (int i = 0; i < 7; i++) deep = group("all", deep);
        search("customers", request(deep)).andExpect(status().isBadRequest());
        search("customers", request(Map.of("kind", "group", "match", "all", "children", Collections.nCopies(101, rule("name", "equals", "x")))))
                .andExpect(status().isBadRequest());
        var body = request(null); body.put("size", 101); search("customers", body).andExpect(status().isBadRequest());
        body.put("size", 20); body.put("sort", List.of(Map.of("field", "passwordHash", "direction", "desc")));
        search("customers", body).andExpect(status().isBadRequest());
    }

    @Test
    void numericOperatorsPreserveDecimalPrecision() throws Exception {
        var value = opportunity(" precise", "9007199254740993.01", "需求確認", "2026-12-31");
        for (String op : List.of("equals", "greaterThanOrEqual", "lessThanOrEqual")) {
            search("opportunities", request(group("all", rule("id", "equals", value.getId()),
                    rule("amount", op, new BigDecimal("9007199254740993.01")))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        }
        for (String op : List.of("notEquals", "greaterThan", "lessThan")) {
            search("opportunities", request(group("all", rule("id", "equals", value.getId()),
                    rule("amount", op, new BigDecimal("9007199254740993.01")))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
        search("opportunities", request(group("all", rule("id", "equals", value.getId()),
                rule("amount", "greaterThan", new BigDecimal("9007199254740993.00")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
}
