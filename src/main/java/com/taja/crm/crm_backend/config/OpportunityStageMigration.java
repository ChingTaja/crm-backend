package com.taja.crm.crm_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OpportunityStageMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.update("""
            update opportunities set stage = case
              when stage in ('需求確認', '提案報價', '協商中') then '需求討論中'
              when stage = '已成交' then '需求成交'
              when stage = '已失單' then '失單'
              else stage end
            where stage in ('需求確認', '提案報價', '協商中', '已成交', '已失單')
            """);
    }
}
