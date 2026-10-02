package com.taja.crm.crm_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 補上 Hibernate 無法以一般 unique annotation 表示的大小寫不敏感唯一索引。 */
@Component
@RequiredArgsConstructor
public class ProductSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_products_sku_ci ON products (lower(btrim(sku)))");
    }
}
