package com.taja.crm.crm_backend.config;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class QuoteSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override public void run(ApplicationArguments args) {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS quote_number_seq START WITH 1");
    }
}
