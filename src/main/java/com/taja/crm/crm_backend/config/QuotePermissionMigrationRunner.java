package com.taja.crm.crm_backend.config;
import com.taja.crm.crm_backend.service.QuotePermissionMigration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
@Component @Order(10) @RequiredArgsConstructor
public class QuotePermissionMigrationRunner implements ApplicationRunner {
 private final QuotePermissionMigration migration;
 @Override public void run(ApplicationArguments args) {migration.migrate();}
}
