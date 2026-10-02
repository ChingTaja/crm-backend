package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.quote.*;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class QuoteRollbackTests {
    @Autowired QuoteService service;
    @Autowired SalesOrderRepository orders;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean QuoteRepository quotes;

    @Test void failureAfterOrderInsertRollsBackTheWholeConversion() {
        long beforeOrders=orders.count(), beforeQuotes=quotes.count();
        TransactionTemplate transaction=new TransactionTemplate(transactions);
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            User user=new User(); user.setUsername("rollback-"+UUID.randomUUID());
            user.setEmail(user.getUsername()+"@example.com"); user.setPasswordHash("unused"); users.saveAndFlush(user);
            Customer customer=new Customer(); customer.setName("rollback"); customers.saveAndFlush(customer);
            Product product=new Product(); product.setName("rollback"); product.setSku(UUID.randomUUID().toString());
            product.setStatus("啟用"); product.setPrice(BigDecimal.ONE); products.saveAndFlush(product);
            CreateQuoteRequest request=new CreateQuoteRequest(); request.setName("rollback"); request.setCustomerId(customer.getId());
            request.setValidUntil(LocalDate.now().plusDays(1));
            request.setLines(List.of(new QuoteLineRequest(null, product.getId(),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO)));
            QuoteResponse quote=service.createQuotes(user.getId(),request);
            String version=quote.versions().getFirst().id();
            service.send(user.getId(),quote.id(),version,new QuoteActionRequest(1L));
            service.decision(user.getId(),quote.id(),version,new DecideQuoteRequest(2L,"accepted",null));
            doAnswer(invocation -> {
                assertEquals(beforeOrders+1,orders.count(), "order INSERT happened before simulated backwrite failure");
                throw new IllegalStateException("simulated quote backwrite failure");
            }).when(quotes).flush();
            return service.convertToOrder(user.getId(),quote.id(),version,new QuoteActionRequest(3L));
        }));
        assertEquals(beforeOrders,orders.count());
        assertEquals(beforeQuotes,quotes.count());
    }
}
