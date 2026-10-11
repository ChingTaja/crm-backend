package com.taja.crm.crm_backend.config;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Order(20) @RequiredArgsConstructor
public class EntityReferenceMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        jdbc.execute(new ClassPathResource("db/migration/manual_entity_reference_guards.sql").getContentAsString(StandardCharsets.UTF_8));
    }
}
