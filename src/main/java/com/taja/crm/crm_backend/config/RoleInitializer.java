package com.taja.crm.crm_backend.config;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.core.annotation.Order(0)
@Component
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.update("INSERT INTO roles (id, code, name, revision) VALUES (?, 'USER', '一般使用者', 1) ON CONFLICT (code) DO NOTHING",
                UUID.randomUUID().toString());
        jdbc.update("INSERT INTO roles (id, code, name, revision) VALUES (?, 'ADMIN', '管理員', 1) ON CONFLICT (code) DO NOTHING",
                UUID.randomUUID().toString());
        jdbc.update("INSERT INTO roles (id, code, name, revision) VALUES (?, 'MANAGER', '主管', 1) ON CONFLICT (code) DO NOTHING",
                UUID.randomUUID().toString());
        jdbc.update("UPDATE users SET role_id = (SELECT id FROM roles WHERE code = 'USER') WHERE role_id IS NULL");
    }
}
