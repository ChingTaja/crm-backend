package com.taja.crm.crm_backend.controller;

import com.taja.crm.crm_backend.model.Contact;
import com.taja.crm.crm_backend.model.Customer;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.repo.ContactRepository;
import com.taja.crm.crm_backend.repo.CustomerRepository;
import com.taja.crm.crm_backend.repo.LeadRepository;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaginationTests extends JwtTestSupport {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LeadRepository leads;
    @MockitoBean
    private CustomerRepository customers;
    @MockitoBean
    private ContactRepository contacts;

    private <T> void stubPage(JpaRepository<T, String> repository, T entity) {
        when(repository.findAll(any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(0);
            assertEquals("id: ASC", pageable.getSort().toString());
            return new PageImpl<>(pageable.getOffset() < 3 ? List.of(entity) : List.of(), pageable, 3);
        });
    }

    private void stubPages() {
        stubPage(leads, new Lead());
        stubPage(customers, new Customer());
        stubPage(contacts, new Contact());
    }

    @ParameterizedTest
    @ValueSource(strings = {"leads", "customers", "contacts"})
    void forwardsPageAndSizeToRepositoryAndReturnsMetadata(String entity) throws Exception {
        stubPages();
        mvc.perform(get("/api/" + entity).header("Authorization", bearerToken).param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"leads", "customers", "contacts"})
    void defaultsToFirstPageWithTwentyItems(String entity) throws Exception {
        stubPages();
        mvc.perform(get("/api/" + entity).header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"leads", "customers", "contacts"})
    void outOfRangePageReturnsEmptyContentWithTotals(String entity) throws Exception {
        stubPages();
        mvc.perform(get("/api/" + entity).header("Authorization", bearerToken).param("page", "10").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"leads", "customers", "contacts"})
    void rejectsInvalidPaginationBeforeQueryingDatabase(String entity) throws Exception {
        for (String query : List.of("?page=-1", "?size=0", "?size=101", "?page=abc")) {
            mvc.perform(get("/api/" + entity + query).header("Authorization", bearerToken)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(leads, customers, contacts);
    }
}
